package com.example.aiquestionassistant.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Base64
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.aiquestionassistant.service.FloatingBubbleService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.concurrent.atomic.AtomicBoolean

class ScreenCaptureActivity : ComponentActivity() {

    private var mediaProjectionManager: MediaProjectionManager? = null
    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var capturedBitmap: Bitmap? = null
    private var captureThread: HandlerThread? = null
    private var cropPreviewOverlay: FrameLayout? = null

    private val isCaptured = AtomicBoolean(false)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val timeoutHandler = Handler(Looper.getMainLooper())

    private val captureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val resultCode = result.resultCode
        val data = result.data
        if (resultCode == RESULT_OK && data != null) {
            try {
                // Em Android 14+ (API 34+), o serviço em primeiro plano precisa do tipo mediaProjection
                if (FloatingBubbleService.instance == null) {
                    val serviceIntent = Intent(this, FloatingBubbleService::class.java).apply {
                        putExtra("enable_projection", true)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        ContextCompat.startForegroundService(this, serviceIntent)
                    } else {
                        startService(serviceIntent)
                    }
                } else {
                    FloatingBubbleService.instance?.enableMediaProjectionForeground()
                }

                // Esconde a bolha temporariamente para não aparecer no print
                FloatingBubbleService.instance?.setBubbleVisibility(false)

                // Intervalo para o foreground service promover e a tela estabilizar
                mainHandler.postDelayed({
                    startScreenCapture(resultCode, data)
                }, 250)
            } catch (t: Throwable) {
                t.printStackTrace()
                handleCaptureFailure("Erro ao inicializar captura: ${t.message}")
            }
        } else {
            FloatingBubbleService.instance?.disableMediaProjectionForeground()
            FloatingBubbleService.instance?.setBubbleVisibility(true)
            Toast.makeText(this, "Captura de ecrã cancelada", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Janela translúcida e imersiva para cobrir toda a área
        window.addFlags(
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }

        mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val intent = mediaProjectionManager?.createScreenCaptureIntent()
        if (intent != null) {
            captureLauncher.launch(intent)
        } else {
            Toast.makeText(this, "MediaProjection indisponível no dispositivo", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun startScreenCapture(resultCode: Int, data: android.content.Intent) {
        try {
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val (width, height, density) = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bounds = windowManager.currentWindowMetrics.bounds
                Triple(bounds.width(), bounds.height(), resources.configuration.densityDpi)
            } else {
                val metrics = DisplayMetrics()
                @Suppress("DEPRECATION")
                windowManager.defaultDisplay.getRealMetrics(metrics)
                Triple(metrics.widthPixels, metrics.heightPixels, metrics.densityDpi)
            }

            if (width <= 0 || height <= 0) {
                handleCaptureFailure("Dimensões do ecrã inválidas")
                return
            }

            // Thread em segundo plano dedicada para processamento de buffer gráfico
            captureThread = HandlerThread("ScreenCaptureThread").apply { start() }
            val backgroundHandler = Handler(captureThread!!.looper)

            imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
            mediaProjection = mediaProjectionManager?.getMediaProjection(resultCode, data)

            if (mediaProjection == null) {
                handleCaptureFailure("Falha ao obter permissão de projeção do ecrã")
                return
            }

            // Android 14+ exige estritamente o registo de callback antes de criar o VirtualDisplay
            mediaProjection?.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    super.onStop()
                    stopProjection()
                }
            }, backgroundHandler)

            // Timeout de segurança (4s) caso o ecrã esteja estático ou o sistema bloqueie
            timeoutHandler.postDelayed({
                if (isCaptured.compareAndSet(false, true)) {
                    stopProjection()
                    handleCaptureFailure("Tempo limite excedido na captura do ecrã")
                }
            }, 4000)

            virtualDisplay = mediaProjection?.createVirtualDisplay(
                "ScreenCapture",
                width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface, null, backgroundHandler
            )

            imageReader?.setOnImageAvailableListener({ reader ->
                try {
                    val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                    try {
                        if (isCaptured.compareAndSet(false, true)) {
                            timeoutHandler.removeCallbacksAndMessages(null)

                            val planes = image.planes
                            if (planes.isEmpty()) {
                                mainHandler.post {
                                    handleCaptureFailure("Buffer de imagem não disponível")
                                }
                                return@setOnImageAvailableListener
                            }
                            val buffer = planes[0].buffer
                            val pixelStride = planes[0].pixelStride
                            val rowStride = planes[0].rowStride
                            if (pixelStride <= 0) {
                                mainHandler.post {
                                    handleCaptureFailure("Estrutura de píxeis incompatível")
                                }
                                return@setOnImageAvailableListener
                            }
                            val rowPadding = rowStride - pixelStride * width

                            val bmpWidth = (width + rowPadding / pixelStride).coerceAtLeast(1)
                            val rawBitmap = Bitmap.createBitmap(
                                bmpWidth,
                                height,
                                Bitmap.Config.ARGB_8888
                            )
                            rawBitmap.copyPixelsFromBuffer(buffer)
                            val cleanBitmap = Bitmap.createBitmap(rawBitmap, 0, 0, width.coerceAtMost(rawBitmap.width), height.coerceAtMost(rawBitmap.height))
                            if (rawBitmap != cleanBitmap) {
                                rawBitmap.recycle()
                            }
                            capturedBitmap = cleanBitmap

                            // Para a projeção imediatamente para libertar recursos de hardware
                            stopProjection()

                            mainHandler.post {
                                showCropOverlay(cleanBitmap)
                            }
                        }
                    } finally {
                        try {
                            image.close()
                        } catch (_: Exception) {}
                    }
                } catch (e: Throwable) {
                    e.printStackTrace()
                    mainHandler.post {
                        handleCaptureFailure("Erro ao extrair imagem do ecrã: ${e.message}")
                    }
                }
            }, backgroundHandler)
        } catch (e: SecurityException) {
            e.printStackTrace()
            handleCaptureFailure("Permissão de projeção negada pelo sistema: ${e.message}")
        } catch (t: Throwable) {
            t.printStackTrace()
            handleCaptureFailure("Erro ao iniciar captura do ecrã: ${t.message}")
        }
    }

    private fun showCropOverlay(cleanBitmap: Bitmap) {
        val root = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        // Camada interativa de corte com a imagem estática congelada
        val cropView = CropOverlayView(
            context = this,
            bitmap = cleanBitmap,
            onCropSelected = { rect ->
                processCroppedSelection(rect, isFullScreen = false)
            },
            onCancel = {
                cancelCapture()
            }
        )
        root.addView(cropView)

        // Barra inferior de atalhos rápidos com design moderno
        val bottomBar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dpToPx(24f)
                setColor(Color.parseColor("#EE090D16")) // Glass escuro
                setStroke(dpToPx(1f).toInt(), Color.parseColor("#3338BDF8"))
            }
            val padH = dpToPx(16f).toInt()
            val padV = dpToPx(10f).toInt()
            setPadding(padH, padV, padH, padV)
            elevation = dpToPx(12f)

            // Botão 1: Ecrã Inteiro
            val btnFullScreen = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    processCroppedSelection(null, isFullScreen = true)
                }

                val icon = ImageView(context).apply {
                    setImageResource(com.example.aiquestionassistant.R.drawable.ic_fullscreen)
                    setColorFilter(Color.parseColor("#38BDF8"))
                }
                addView(icon, LinearLayout.LayoutParams(dpToPx(20f).toInt(), dpToPx(20f).toInt()).apply {
                    setMargins(0, 0, dpToPx(6f).toInt(), 0)
                })

                val label = TextView(context).apply {
                    text = "Ecrã Inteiro"
                    setTextColor(Color.parseColor("#38BDF8"))
                    textSize = 13f
                    typeface = Typeface.DEFAULT_BOLD
                }
                addView(label)
            }
            addView(btnFullScreen)

            // Divisor vertical
            val divider = TextView(context).apply {
                text = "|"
                setTextColor(Color.parseColor("#334155"))
                textSize = 14f
                setPadding(dpToPx(12f).toInt(), 0, dpToPx(12f).toInt(), 0)
            }
            addView(divider)

            // Botão 2: Cancelar
            val btnCancel = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    cancelCapture()
                }

                val icon = ImageView(context).apply {
                    setImageResource(com.example.aiquestionassistant.R.drawable.ic_close)
                    setColorFilter(Color.parseColor("#EF4444"))
                }
                addView(icon, LinearLayout.LayoutParams(dpToPx(18f).toInt(), dpToPx(18f).toInt()).apply {
                    setMargins(0, 0, dpToPx(4f).toInt(), 0)
                })

                val label = TextView(context).apply {
                    text = "Cancelar"
                    setTextColor(Color.parseColor("#EF4444"))
                    textSize = 13f
                    typeface = Typeface.DEFAULT_BOLD
                }
                addView(label)
            }
            addView(btnCancel)
        }

        val barParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.WRAP_CONTENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            bottomMargin = dpToPx(44f).toInt()
        }
        root.addView(bottomBar, barParams)

        setContentView(root)
    }

    private fun processCroppedSelection(rect: RectF?, isFullScreen: Boolean) {
        val fullBmp = capturedBitmap ?: run {
            handleCaptureFailure("Erro: imagem não disponível")
            return
        }

        val viewW = window.decorView.width.takeIf { it > 0 } ?: resources.displayMetrics.widthPixels
        val viewH = window.decorView.height.takeIf { it > 0 } ?: resources.displayMetrics.heightPixels

        val (cropX, cropY, cropW, cropH) = if (isFullScreen || rect == null) {
            listOf(0, 0, fullBmp.width, fullBmp.height)
        } else {
            val scaleX = fullBmp.width.toFloat() / viewW.toFloat()
            val scaleY = fullBmp.height.toFloat() / viewH.toFloat()

            val left = (rect.left * scaleX).toInt().coerceIn(0, fullBmp.width - 1)
            val top = (rect.top * scaleY).toInt().coerceIn(0, fullBmp.height - 1)
            val w = (rect.width() * scaleX).toInt().coerceIn(1, fullBmp.width - left)
            val h = (rect.height() * scaleY).toInt().coerceIn(1, fullBmp.height - top)
            listOf(left, top, w, h)
        }

        try {
            val croppedBitmap = if (isFullScreen || (cropX == 0 && cropY == 0 && cropW == fullBmp.width && cropH == fullBmp.height)) {
                fullBmp
            } else {
                Bitmap.createBitmap(fullBmp, cropX, cropY, cropW, cropH)
            }
            showCroppedPreviewConfirmation(croppedBitmap, isFullScreen)
        } catch (e: Throwable) {
            e.printStackTrace()
            handleCaptureFailure("Erro ao recortar imagem: ${e.message}")
        }
    }

    private fun showCroppedPreviewConfirmation(croppedBmp: Bitmap, isFullScreen: Boolean) {
        val root = findViewById<ViewGroup>(android.R.id.content) ?: return
        cropPreviewOverlay?.let { root.removeView(it) }

        val overlay = FrameLayout(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(Color.parseColor("#B3000000"))
            isClickable = true
            isFocusable = true
        }

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dpToPx(20f)
                setColor(Color.parseColor("#F5090D16"))
                setStroke(dpToPx(1.5f).toInt(), Color.parseColor("#38BDF8"))
            }
            val pad = dpToPx(16f).toInt()
            setPadding(pad, pad, pad, pad)
            elevation = dpToPx(16f)
        }

        // Cabeçalho da pré-visualização
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            val icon = ImageView(context).apply {
                setImageResource(com.example.aiquestionassistant.R.drawable.ic_camera)
                setColorFilter(Color.parseColor("#38BDF8"))
            }
            addView(icon, LinearLayout.LayoutParams(dpToPx(22f).toInt(), dpToPx(22f).toInt()).apply {
                setMargins(0, 0, dpToPx(8f).toInt(), 0)
            })

            val titleCol = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL

                val title = TextView(context).apply {
                    text = if (isFullScreen) "Pré-visualização (Ecrã Inteiro)" else "Pré-visualização do Corte"
                    setTextColor(Color.WHITE)
                    textSize = 15f
                    typeface = Typeface.DEFAULT_BOLD
                }
                addView(title)

                val subtitle = TextView(context).apply {
                    text = "${croppedBmp.width} × ${croppedBmp.height} px • Pronto para a IA"
                    setTextColor(Color.parseColor("#94A3B8"))
                    textSize = 11f
                }
                addView(subtitle)
            }
            addView(titleCol, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        }
        card.addView(header)

        // Moldura para imagem recortada
        val imageContainer = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                cornerRadius = dpToPx(12f)
                setColor(Color.parseColor("#0B0F19"))
                setStroke(dpToPx(1f).toInt(), Color.parseColor("#1E293B"))
            }
            clipToOutline = true
        }

        val previewImageView = ImageView(this).apply {
            setImageBitmap(croppedBmp)
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
        }
        val maxImgHeight = (resources.displayMetrics.heightPixels * 0.42).toInt().coerceAtLeast(dpToPx(180f).toInt())
        imageContainer.addView(previewImageView, FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        })

        card.addView(imageContainer, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(0, dpToPx(12f).toInt(), 0, dpToPx(14f).toInt())
            height = maxImgHeight
        })

        // Linha com Botões de Ação
        val buttonsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL

            // Botão Recortar Novamente
            val btnRetake = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    cornerRadius = dpToPx(12f)
                    setColor(Color.parseColor("#1E293B"))
                    setStroke(dpToPx(1f).toInt(), Color.parseColor("#475569"))
                }
                setPadding(dpToPx(12f).toInt(), dpToPx(10f).toInt(), dpToPx(12f).toInt(), dpToPx(10f).toInt())
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    root.removeView(overlay)
                    cropPreviewOverlay = null
                    if (croppedBmp != capturedBitmap) {
                        croppedBmp.recycle()
                    }
                }

                val retakeLabel = TextView(context).apply {
                    text = "↺ Recortar"
                    setTextColor(Color.parseColor("#E2E8F0"))
                    textSize = 13f
                    typeface = Typeface.DEFAULT_BOLD
                }
                addView(retakeLabel)
            }
            addView(btnRetake, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply {
                setMargins(0, 0, dpToPx(8f).toInt(), 0)
            })

            // Botão Confirmar e Enviar para IA
            val btnConfirm = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER
                background = GradientDrawable().apply {
                    cornerRadius = dpToPx(12f)
                    setColor(Color.parseColor("#0284C7"))
                }
                setPadding(dpToPx(14f).toInt(), dpToPx(10f).toInt(), dpToPx(14f).toInt(), dpToPx(10f).toInt())
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    root.removeView(overlay)
                    cropPreviewOverlay = null
                    dispatchOptimizedImage(croppedBmp)
                }

                val confirmLabel = TextView(context).apply {
                    text = "✓ Confirmar e Enviar"
                    setTextColor(Color.WHITE)
                    textSize = 13f
                    typeface = Typeface.DEFAULT_BOLD
                }
                addView(confirmLabel)
            }
            addView(btnConfirm, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.4f))
        }
        card.addView(buttonsRow)

        val cardParams = FrameLayout.LayoutParams(
            (resources.displayMetrics.widthPixels * 0.92).toInt().coerceAtMost(dpToPx(480f).toInt()),
            FrameLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.CENTER
        }
        overlay.addView(card, cardParams)

        cropPreviewOverlay = overlay
        root.addView(overlay)
    }

    private fun dispatchOptimizedImage(bitmapToProcess: Bitmap) {
        Toast.makeText(this, "A preparar captura para a IA...", Toast.LENGTH_SHORT).show()

        lifecycleScope.launch(Dispatchers.Default) {
            try {
                val fullBmp = capturedBitmap
                val maxDim = maxOf(bitmapToProcess.width, bitmapToProcess.height)
                val optimizedBitmap = if (maxDim > 1600) {
                    val scale = 1600f / maxDim
                    val targetW = (bitmapToProcess.width * scale).toInt().coerceAtLeast(1)
                    val targetH = (bitmapToProcess.height * scale).toInt().coerceAtLeast(1)
                    val scaled = Bitmap.createScaledBitmap(bitmapToProcess, targetW, targetH, true)
                    if (bitmapToProcess != fullBmp && bitmapToProcess != scaled) {
                        bitmapToProcess.recycle()
                    }
                    scaled
                } else {
                    bitmapToProcess
                }

                val outputStream = ByteArrayOutputStream()
                optimizedBitmap.compress(Bitmap.CompressFormat.JPEG, 82, outputStream)
                val byteArray = outputStream.toByteArray()
                val base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)

                if (optimizedBitmap != fullBmp) {
                    optimizedBitmap.recycle()
                }

                withContext(Dispatchers.Main) {
                    if (FloatingBubbleService.instance != null) {
                        FloatingBubbleService.instance?.let { service ->
                            service.disableMediaProjectionForeground()
                            service.setBubbleVisibility(true)
                            service.processCapturedImage(base64)
                        }
                    } else {
                        val serviceIntent = Intent(this@ScreenCaptureActivity, FloatingBubbleService::class.java).apply {
                            putExtra("shared_image", base64)
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            ContextCompat.startForegroundService(this@ScreenCaptureActivity, serviceIntent)
                        } else {
                            startService(serviceIntent)
                        }
                    }
                    finish()
                }
            } catch (e: Throwable) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    handleCaptureFailure("Erro ao otimizar imagem: ${e.message}")
                }
            }
        }
    }

    private fun cancelCapture() {
        stopProjection()
        FloatingBubbleService.instance?.let { service ->
            service.disableMediaProjectionForeground()
            service.setBubbleVisibility(true)
        }
        finish()
    }

    private fun handleCaptureFailure(errorMsg: String) {
        stopProjection()
        FloatingBubbleService.instance?.let { service ->
            service.disableMediaProjectionForeground()
            service.setBubbleVisibility(true)
        }
        Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun stopProjection() {
        try {
            virtualDisplay?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        virtualDisplay = null

        try {
            imageReader?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        imageReader = null

        try {
            mediaProjection?.stop()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        mediaProjection = null
    }

    private fun cleanupThread() {
        try {
            captureThread?.quitSafely()
            captureThread = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }

    override fun onDestroy() {
        super.onDestroy()
        timeoutHandler.removeCallbacksAndMessages(null)
        mainHandler.removeCallbacksAndMessages(null)
        stopProjection()
        cleanupThread()
        capturedBitmap?.recycle()
        capturedBitmap = null
    }
}
