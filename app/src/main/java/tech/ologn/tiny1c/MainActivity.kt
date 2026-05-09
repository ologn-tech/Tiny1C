package tech.ologn.tiny1c

import android.Manifest
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbConstants
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.TextureView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.UVCCamera

/**
 * Stream format types reported in [UVCCamera.getSupportedSize] JSON ("type" field).
 * See libuvccamera UVCCamera.java — MJPEG uses type 6, uncompressed YUYV uses type 4.
 */
private const val UVC_STREAM_TYPE_YUYV = 4
private const val UVC_STREAM_TYPE_MJPEG = 6

class MainActivity : ComponentActivity() {
    private lateinit var statusText: TextView
    private lateinit var cameraView: TextureView

    private var usbMonitor: USBMonitor? = null
    private var uvcCamera: UVCCamera? = null
    private var requestingPermission = false
    private var usbRegistered = false

    private val deviceConnectListener = object : USBMonitor.OnDeviceConnectListener {
        override fun onAttach(device: UsbDevice?) {
            if (!isUvcDevice(device)) return
            runOnUiThread {
                statusText.text = getString(
                    R.string.usb_camera_attached_with_device,
                    device?.vendorId ?: -1,
                    device?.productId ?: -1
                )
            }
            if (device == null || requestingPermission) return
            requestingPermission = true
            usbMonitor?.requestPermission(device)
        }

        override fun onDettach(device: UsbDevice?) {
            requestingPermission = false
            runOnUiThread {
                statusText.text = getString(R.string.usb_camera_disconnected)
            }
        }

        override fun onConnect(
            device: UsbDevice?,
            ctrlBlock: USBMonitor.UsbControlBlock?,
            createNew: Boolean
        ) {
            if (ctrlBlock == null) return
            requestingPermission = false
            runOnUiThread {
                statusText.text = getString(R.string.usb_camera_connected)
            }
            openCamera(ctrlBlock)
        }

        override fun onDisconnect(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
            requestingPermission = false
            closeCamera()
            runOnUiThread {
                statusText.text = getString(R.string.usb_camera_disconnected)
            }
        }

        override fun onCancel(device: UsbDevice?) {
            requestingPermission = false
            if (!isUvcDevice(device)) return
            runOnUiThread {
                statusText.text = getString(
                    R.string.usb_camera_permission_denied_with_device,
                    device?.vendorId ?: -1,
                    device?.productId ?: -1
                )
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        statusText = findViewById(R.id.statusText)
        cameraView = findViewById(R.id.cameraTextureView)
        usbMonitor = USBMonitor(this, deviceConnectListener)
    }

    override fun onStart() {
        super.onStart()
        ensureCameraPermissionAndStartUsb()
    }

    override fun onStop() {
        closeCamera()
        requestingPermission = false
        if (usbRegistered) {
            usbMonitor?.unregister()
            usbRegistered = false
        }
        super.onStop()
    }

    override fun onDestroy() {
        closeCamera()
        usbMonitor?.destroy()
        usbMonitor = null
        super.onDestroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != REQUEST_CAMERA_PERMISSION) return
        val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
        if (granted) {
            ensureCameraPermissionAndStartUsb()
        } else {
            statusText.text = getString(R.string.camera_runtime_permission_required)
        }
    }

    private fun ensureCameraPermissionAndStartUsb() {
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) {
            statusText.text = getString(R.string.camera_runtime_permission_required)
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
            return
        }
        startUsbFlow()
    }

    private fun startUsbFlow() {
        if (!usbRegistered) {
            usbMonitor?.register()
            usbRegistered = true
        }
        val firstDevice = usbMonitor?.deviceList?.firstOrNull { isUvcDevice(it) }
        if (firstDevice == null) {
            statusText.text = getString(R.string.usb_camera_waiting)
        } else {
            statusText.text = getString(
                R.string.usb_camera_attached_with_device,
                firstDevice.vendorId,
                firstDevice.productId
            )
            requestingPermission = true
            usbMonitor?.requestPermission(firstDevice)
        }
    }

    private fun openCamera(ctrlBlock: USBMonitor.UsbControlBlock) {
        closeCamera()
        val camera = UVCCamera()
        try {
            camera.open(ctrlBlock)
            val supportedJson = camera.supportedSize
            if (supportedJson.isNullOrBlank()) {
                failOpenCamera(camera, getString(R.string.usb_camera_no_supported_size))
                return
            }
            val picked =
                pickPreviewSize(supportedJson) ?: run {
                    failOpenCamera(camera, getString(R.string.usb_camera_no_supported_size))
                    return
                }
            val (width, height, frameFormat) = picked
            try {
                camera.setPreviewSize(width, height, frameFormat)
            } catch (_: IllegalArgumentException) {
                if (!tryFallbackPreviewSizes(camera, supportedJson)) {
                    failOpenCamera(
                        camera,
                        getString(R.string.usb_camera_preview_size_failed),
                    )
                    return
                }
            }
            uvcCamera = camera
            runOnUiThread {
                try {
                    camera.setPreviewTexture(cameraView.surfaceTexture)
                    camera.startPreview()
                } catch (e: Exception) {
                    statusText.text =
                        getString(R.string.usb_camera_error, e.message ?: e.toString())
                    closeCamera()
                }
            }
        } catch (e: IllegalArgumentException) {
            failOpenCamera(camera, e.message ?: e.toString())
        } catch (e: Exception) {
            failOpenCamera(camera, e.message ?: e.toString())
        }
    }

    private fun failOpenCamera(camera: UVCCamera?, message: String) {
        camera?.destroy()
        runOnUiThread {
            statusText.text = getString(R.string.usb_camera_error, message)
        }
    }

    /**
     * Try every advertised size until [UVCCamera.setPreviewSize] succeeds.
     * Ordering: MJPEG (largest first), then YUYV.
     */
    private fun tryFallbackPreviewSizes(camera: UVCCamera, supportedJson: String): Boolean {
        val attempts =
            listOf(
                UVCCamera.FRAME_FORMAT_MJPEG to
                    UVCCamera.getSupportedSize(UVC_STREAM_TYPE_MJPEG, supportedJson),
                UVCCamera.FRAME_FORMAT_YUYV to
                    UVCCamera.getSupportedSize(UVC_STREAM_TYPE_YUYV, supportedJson),
            )
        for ((frameFormat, sizes) in attempts) {
            val ordered = sizes.sortedByDescending { it.width * it.height }
            for (sz in ordered) {
                try {
                    camera.setPreviewSize(sz.width, sz.height, frameFormat)
                    return true
                } catch (_: IllegalArgumentException) {
                    // try next size
                }
            }
        }
        return false
    }

    /** Pick a resolution the device advertises; prefers MJPEG then YUYV. */
    private fun pickPreviewSize(supportedJson: String): Triple<Int, Int, Int>? {
        val mjpegSizes =
            UVCCamera.getSupportedSize(UVC_STREAM_TYPE_MJPEG, supportedJson)
        val yuyvSizes = UVCCamera.getSupportedSize(UVC_STREAM_TYPE_YUYV, supportedJson)
        val preferredDims = listOf(
            1920 to 1080,
            1280 to 720,
            1024 to 768,
            800 to 600,
            720 to 480,
            640 to 480,
            640 to 360,
            352 to 288,
            320 to 240
        )
        for ((w, h) in preferredDims) {
            if (mjpegSizes.any { it.width == w && it.height == h }) {
                return Triple(w, h, UVCCamera.FRAME_FORMAT_MJPEG)
            }
        }
        if (mjpegSizes.isNotEmpty()) {
            val best = mjpegSizes.maxByOrNull { it.width * it.height }!!
            return Triple(best.width, best.height, UVCCamera.FRAME_FORMAT_MJPEG)
        }
        for ((w, h) in preferredDims) {
            if (yuyvSizes.any { it.width == w && it.height == h }) {
                return Triple(w, h, UVCCamera.FRAME_FORMAT_YUYV)
            }
        }
        if (yuyvSizes.isNotEmpty()) {
            val best = yuyvSizes.maxByOrNull { it.width * it.height }!!
            return Triple(best.width, best.height, UVCCamera.FRAME_FORMAT_YUYV)
        }
        return null
    }

    private fun closeCamera() {
        uvcCamera?.apply {
            stopPreview()
            destroy()
        }
        uvcCamera = null
    }

    private fun isUvcDevice(device: UsbDevice?): Boolean {
        if (device == null) return false
        if (device.deviceClass == UsbConstants.USB_CLASS_VIDEO) return true
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            if (iface.interfaceClass == UsbConstants.USB_CLASS_VIDEO) {
                return true
            }
        }
        return false
    }

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 1001
    }
}