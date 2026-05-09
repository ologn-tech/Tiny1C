package tech.ologn.tiny1c

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.TextureView
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.infisense.iruvc.sdkisp.Libirprocess
import com.serenegiant.usb.IFrameCallback
import com.serenegiant.usb.USBMonitor
import com.serenegiant.usb.UVCCamera
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * UVC stream types in [UVCCamera.getSupportedSize] JSON ("type" field). MJPEG = 6, YUYV = 4.
 */
private const val UVC_STREAM_TYPE_YUYV = 4
private const val UVC_STREAM_TYPE_MJPEG = 6

class MainActivity : ComponentActivity() {
    private lateinit var statusText: TextView
    private lateinit var cameraView: TextureView
    private lateinit var previewImage: ImageView
    private lateinit var pseudoColorGrid: GridLayout
    private lateinit var pseudoColorOptions: List<RadioButton>
    private lateinit var pseudoColorModeValues: IntArray

    private var usbMonitor: USBMonitor? = null
    private var uvcCamera: UVCCamera? = null
    private var requestingPermission = false
    private var usbRegistered = false

    private var previewWidth = 0
    private var previewHeight = 0
    private var activeFrameFormat: Int = -1
    private var argbBuffer: ByteArray? = null
    private var displayBitmap: Bitmap? = null
    private var frameExecutor: ExecutorService? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val selectedPseudoMode = AtomicInteger(3)

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
        previewImage = findViewById(R.id.previewImage)
        pseudoColorGrid = findViewById(R.id.pseudoColorGrid)
        usbMonitor = USBMonitor(this, deviceConnectListener)
        setupPseudoColorGrid()
    }

    private fun setupPseudoColorGrid() {
        val labels = resources.getStringArray(R.array.pseudo_color_mode_labels)
        pseudoColorModeValues = resources.getIntArray(R.array.pseudo_color_mode_values)
        require(labels.size == pseudoColorModeValues.size) {
            "Pseudo color label/value arrays must have matching length"
        }
        pseudoColorGrid.removeAllViews()
        val whiteTint = ContextCompat.getColorStateList(this, android.R.color.white)
        val whiteColor = ContextCompat.getColor(this, android.R.color.white)
        val horizontalPadding = (12 * resources.displayMetrics.density).toInt()
        val buttonMargin = (4 * resources.displayMetrics.density).toInt()
        val minHeightPx = (48 * resources.displayMetrics.density).toInt()
        pseudoColorOptions = labels.mapIndexed { index, label ->
            RadioButton(this).apply {
                id = View.generateViewId()
                text = label
                setTextColor(whiteColor)
                buttonTintList = whiteTint
                minHeight = minHeightPx
                setPadding(0, 0, horizontalPadding, 0)
                setOnClickListener {
                    selectPseudoColor(index)
                }
                val lp = GridLayout.LayoutParams().apply {
                    width = 0
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    setMargins(0, 0, buttonMargin, buttonMargin)
                }
                layoutParams = lp
                pseudoColorGrid.addView(this)
            }
        }
        val initialIndex = pseudoColorModeValues
            .indexOfFirst { it == selectedPseudoMode.get() }
            .takeIf { it >= 0 } ?: 0
        selectPseudoColor(initialIndex)
    }

    private fun selectPseudoColor(index: Int) {
        selectedPseudoMode.set(pseudoColorModeValues[index])
        pseudoColorOptions.forEachIndexed { optionIndex, button ->
            button.isChecked = optionIndex == index
        }
    }

    private fun setPseudoColorEnabled(enabled: Boolean) {
        pseudoColorGrid.isEnabled = enabled
        pseudoColorOptions.forEach { it.isEnabled = enabled }
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
        frameExecutor?.shutdownNow()
        frameExecutor = null
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
                pickPreviewSizePreferYuyv(supportedJson) ?: run {
                    failOpenCamera(camera, getString(R.string.usb_camera_no_supported_size))
                    return
                }
            val (width, height, frameFormat) = picked
            previewWidth = width
            previewHeight = height
            activeFrameFormat = frameFormat
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
                    if (activeFrameFormat == UVCCamera.FRAME_FORMAT_YUYV) {
                        startYuyvThermalPreview(camera)
                    } else {
                        startMjpegTexturePreview(camera)
                    }
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

    private fun startMjpegTexturePreview(camera: UVCCamera) {
        previewImage.visibility = View.GONE
        cameraView.alpha = 1f
        setPseudoColorEnabled(false)
        statusText.text =
            getString(R.string.usb_camera_connected) + "\n" +
                getString(R.string.pseudo_colors_need_yuyv)
        camera.setFrameCallback(null, 0)
        camera.setPreviewTexture(cameraView.surfaceTexture)
        camera.startPreview()
    }

    /**
     * Same pipeline as Thermography [ImageThread]: YUYV → libirprocess → ARGB bitmap.
     * Stock app uses [Libirprocess.yuyv_map_to_argb_pseudocolor] with IRPROC_COLOR_MODE_*.
     */
    private fun startYuyvThermalPreview(camera: UVCCamera) {
        val w = previewWidth
        val h = previewHeight
        if (w <= 0 || h <= 0) {
            startMjpegTexturePreview(camera)
            return
        }
        val pixelCount = w * h
        val yuyvBytes = pixelCount * 2
        argbBuffer = ByteArray(pixelCount * 4)
        displayBitmap?.recycle()
        displayBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        frameExecutor?.shutdownNow()
        frameExecutor = Executors.newSingleThreadExecutor()

        previewImage.visibility = View.VISIBLE
        cameraView.alpha = 0f
        setPseudoColorEnabled(true)
        statusText.text = getString(R.string.usb_camera_connected)
        camera.setPreviewTexture(cameraView.surfaceTexture)
        camera.setFrameCallback(
            IFrameCallback { frame: ByteBuffer ->
                if (frame.remaining() < yuyvBytes) return@IFrameCallback
                val copy = ByteArray(yuyvBytes)
                val dup = frame.duplicate()
                dup.get(copy, 0, yuyvBytes)
                val out = argbBuffer ?: return@IFrameCallback
                val mode = selectedPseudoMode.get()
                frameExecutor?.execute {
                    try {
                        Libirprocess.yuyv_map_to_argb_pseudocolor(
                            copy,
                            pixelCount.toLong(),
                            mode,
                            out
                        )
                        val bmp = displayBitmap ?: return@execute
                        bmp.copyPixelsFromBuffer(ByteBuffer.wrap(out))
                        mainHandler.post { previewImage.setImageBitmap(bmp) }
                    } catch (_: Throwable) {
                        // skip bad frame
                    }
                }
            },
            UVCCamera.PIXEL_FORMAT_YUV
        )
        camera.startPreview()
    }

    private fun failOpenCamera(camera: UVCCamera?, message: String) {
        camera?.destroy()
        runOnUiThread {
            statusText.text = getString(R.string.usb_camera_error, message)
        }
    }

    private fun tryFallbackPreviewSizes(camera: UVCCamera, supportedJson: String): Boolean {
        val attempts =
            listOf(
                UVCCamera.FRAME_FORMAT_YUYV to
                    UVCCamera.getSupportedSize(UVC_STREAM_TYPE_YUYV, supportedJson),
                UVCCamera.FRAME_FORMAT_MJPEG to
                    UVCCamera.getSupportedSize(UVC_STREAM_TYPE_MJPEG, supportedJson),
            )
        for ((frameFormat, sizes) in attempts) {
            val ordered = sizes.sortedByDescending { it.width * it.height }
            for (sz in ordered) {
                try {
                    camera.setPreviewSize(sz.width, sz.height, frameFormat)
                    previewWidth = sz.width
                    previewHeight = sz.height
                    activeFrameFormat = frameFormat
                    return true
                } catch (_: IllegalArgumentException) {
                    // try next size
                }
            }
        }
        return false
    }

    /** Prefer YUYV (thermal / pseudo); MJPEG as fallback for ordinary USB cameras. */
    private fun pickPreviewSizePreferYuyv(supportedJson: String): Triple<Int, Int, Int>? {
        val mjpegSizes =
            UVCCamera.getSupportedSize(UVC_STREAM_TYPE_MJPEG, supportedJson)
        val yuyvSizes =
            UVCCamera.getSupportedSize(UVC_STREAM_TYPE_YUYV, supportedJson)
        val preferredDims = listOf(
            256 to 192,
            256 to 384,
            384 to 288,
            640 to 480,
            640 to 360,
            1280 to 720,
            160 to 120,
            320 to 240,
            352 to 288,
            800 to 600,
            1024 to 768,
            1920 to 1080,
            720 to 480
        )
        for ((w, h) in preferredDims) {
            if (yuyvSizes.any { it.width == w && it.height == h }) {
                return Triple(w, h, UVCCamera.FRAME_FORMAT_YUYV)
            }
        }
        if (yuyvSizes.isNotEmpty()) {
            val best = yuyvSizes.maxByOrNull { it.width * it.height }!!
            return Triple(best.width, best.height, UVCCamera.FRAME_FORMAT_YUYV)
        }
        for ((w, h) in preferredDims) {
            if (mjpegSizes.any { it.width == w && it.height == h }) {
                return Triple(w, h, UVCCamera.FRAME_FORMAT_MJPEG)
            }
        }
        if (mjpegSizes.isNotEmpty()) {
            val best = mjpegSizes.maxByOrNull { it.width * it.height }!!
            return Triple(best.width, best.height, UVCCamera.FRAME_FORMAT_MJPEG)
        }
        return null
    }

    private fun closeCamera() {
        uvcCamera?.apply {
            try {
                setFrameCallback(null, 0)
            } catch (_: Exception) {
            }
            stopPreview()
            destroy()
        }
        uvcCamera = null
        frameExecutor?.shutdownNow()
        frameExecutor = null
        argbBuffer = null
        displayBitmap?.recycle()
        displayBitmap = null
        previewWidth = 0
        previewHeight = 0
        activeFrameFormat = -1
        runOnUiThread {
            previewImage.visibility = View.GONE
            previewImage.setImageDrawable(null)
            cameraView.alpha = 1f
            setPseudoColorEnabled(true)
        }
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
