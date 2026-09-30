package com.example.aiquestionassistant.capture

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.RectF
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.util.DisplayMetrics
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import com.example.aiquestionassistant.service.FloatingBubbleService
import java.io.ByteArrayOutputStream

class ScreenCaptureActivity : Activity() {

    companion object {
        private const val REQUEST_MEDIA_PROJECTION = 1001
    }

    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var capturedBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make activity full transparent screen
        window.addFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )

        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        startActivityForResult(mediaProjectionManager!!.createScreenCaptureIntent(), REQUEST_MEDIA_PROJECTION)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_MEDIA_PROJECTION) {
            if (resultCode == RESULT_OK && data != null) {
                // Temporarily hide the floating bubble while capturing
                FloatingBubbleService.instance?.setBubbleVisibility(false)

                // Brief delay to ensure UI updates before capturing
                Handler(Looper.getMainLooper()).postDelayed({
                    startScreenCapture(resultCode, data)
                }, 150)
            } else {
                Toast.makeText(this, "Permissão de captura cancelada", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun startScreenCapture(resultCode: Int, data: Intent) {
        val metrics = DisplayMetrics()
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getRealMetrics(metrics)

        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, data)

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, null
        )

        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            if (image != null && capturedBitmap == null) {
                val planes = image.planes
                val buffer = planes[0].buffer
                val pixelStride = planes[0].pixelStride
                val rowStride = planes[0].rowStride
                val rowPadding = rowStride - pixelStride * width

                val bitmap = Bitmap.createBitmap(
                    width + rowPadding / pixelStride,
                    height,
                    Bitmap.Config.ARGB_8888
                )
                bitmap.copyPixelsFromBuffer(buffer)
                capturedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height)
                image.close()

                runOnUiThread {
                    stopProjection()
                    showCropOverlay(width, height)
                }
            }
        }, Handler(Looper.getMainLooper()))
    }

    private fun showCropOverlay(width: Int, height: Int) {
        val root = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val cropView = CropOverlayView(
            context = this,
            onCropSelected = { rect ->
                processCroppedSelection(rect)
            },
            onCancel = {
                FloatingBubbleService.instance?.setBubbleVisibility(true)
                finish()
            }
        )

        root.addView(cropView)
        setContentView(root)
    }

    private fun processCroppedSelection(rect: RectF) {
        val fullBmp = capturedBitmap
        if (fullBmp == null) {
            Toast.makeText(this, "Erro ao processar imagem", Toast.LENGTH_SHORT).show()
            FloatingBubbleService.instance?.setBubbleVisibility(true)
            finish()
            return
        }

        val cropX = rect.left.toInt().coerceIn(0, fullBmp.width - 1)
        val cropY = rect.top.toInt().coerceIn(0, fullBmp.height - 1)
        val cropW = rect.width().toInt().coerceIn(1, fullBmp.width - cropX)
        val cropH = rect.height().toInt().coerceIn(1, fullBmp.height - cropY)

        val croppedBitmap = Bitmap.createBitmap(fullBmp, cropX, cropY, cropW, cropH)

        val outputStream = ByteArrayOutputStream()
        croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        val byteArray = outputStream.toByteArray()
        val base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)

        // Show bubble again and pass base64 to service for processing
        FloatingBubbleService.instance?.let { service ->
            service.setBubbleVisibility(true)
            service.processCapturedImage(base64)
        }

        finish()
    }

    private fun stopProjection() {
        try {
            virtualDisplay?.release()
            imageReader?.close()
            mediaProjection?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopProjection()
        capturedBitmap?.recycle()
    }
}
