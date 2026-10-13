package com.example.aiquestionassistant.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Base64
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.aiquestionassistant.MainActivity
import com.example.aiquestionassistant.capture.ScreenCaptureActivity
import com.example.aiquestionassistant.data.AiRepository
import com.example.aiquestionassistant.data.AppSettings
import com.example.aiquestionassistant.data.AssistantActionOptions
import com.example.aiquestionassistant.data.AssistantActionType
import com.example.aiquestionassistant.data.AssistantResult
import com.example.aiquestionassistant.data.HistoryRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

class FloatingBubbleService : Service(), TextToSpeech.OnInitListener {

    companion object {
        var instance: FloatingBubbleService? = null
        var isRunning = false
        private const val CHANNEL_ID = "ai_assistant_service_channel"
        private const val NOTIFICATION_ID = 2001
    }

    private var windowManager: WindowManager? = null
    private var bubbleView: FrameLayout? = null
    private var assistantPanelView: LinearLayout? = null

    private var bubbleParams: WindowManager.LayoutParams? = null
    private var panelParams: WindowManager.LayoutParams? = null

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var currentExecutionJob: Job? = null
    private val aiRepository = AiRepository()
    private lateinit var historyRepository: HistoryRepository

    // Contexto atual capturado / compartilhado
    private var currentCapturedImageBase64: String? = null
    private var currentCapturedText: String? = null
    private var lastResult: AssistantResult? = null

    // Componentes de voz
    private var tts: TextToSpeech? = null
    private var isTtsSpeaking = false
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListeningSpeech = false
    private var basePromptTextBeforeSpeech = ""

    // Estado da janela
    private var isExpanded = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        isRunning = false

        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Permissão de sobreposição necessária para o assistente flutuante", Toast.LENGTH_LONG).show()
            stopSelf()
            return
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        historyRepository = HistoryRepository(this)

        // Inicializar TTS nativo
        tts = TextToSpeech(this, this)

        createNotificationChannel()
        startServiceForegroundCompat()
        isRunning = true

        createFloatingBubble()
    }

    private fun startServiceForegroundCompat() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
                } else {
                    startForeground(NOTIFICATION_ID, buildNotification())
                }
            } else {
                startForeground(NOTIFICATION_ID, buildNotification())
            }
        } catch (_: Exception) {
            try {
                startForeground(NOTIFICATION_ID, buildNotification())
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun enableMediaProjectionForeground() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    buildNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun disableMediaProjectionForeground() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIFICATION_ID,
                    buildNotification(),
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            val sharedText = intent.getStringExtra("shared_text")
            val sharedImage = intent.getStringExtra("shared_image")
            if (!sharedText.isNullOrBlank() || !sharedImage.isNullOrBlank()) {
                setSharedContent(sharedText, sharedImage)
            }
        }
        return START_STICKY
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("pt", "PT"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale("pt", "BR"))
            }
            val settings = AppSettings.load(this)
            tts?.setSpeechRate(settings.ttsSpeechRate)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Assistente IA Flutuante",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Mantém o assistente de IA ativo sobre outros aplicativos"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        return builder
            .setContentTitle("Assistente IA Global Ativo")
            .setContentText("Toque no botão flutuante para analisar ecrã, resumir ou explicar")
            .setSmallIcon(com.example.aiquestionassistant.R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createFloatingBubble() {
        if (!Settings.canDrawOverlays(this)) return

        if (bubbleView != null) {
            try {
                windowManager?.removeView(bubbleView)
            } catch (_: Exception) {}
            bubbleView = null
        }

        val settings = AppSettings.load(this)
        val size = dpToPx(settings.bubbleSize)

        bubbleView = FrameLayout(this).apply {
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                colors = intArrayOf(Color.parseColor("#06B6D4"), Color.parseColor("#4F46E5"))
                gradientType = GradientDrawable.LINEAR_GRADIENT
                setStroke(2, Color.parseColor("#FFFFFF"))
            }
            background = bg
            elevation = 16f

            // Logotipo oficial do app no botão flutuante
            val icon = ImageView(context).apply {
                setImageResource(com.example.aiquestionassistant.R.drawable.app_logo)
                scaleType = ImageView.ScaleType.FIT_CENTER
                val pad = dpToPx(3)
                setPadding(pad, pad, pad, pad)
            }
            addView(icon, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        }

        bubbleParams = WindowManager.LayoutParams(
            size, size,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = settings.bubbleX
            y = settings.bubbleY
        }

        setupBubbleTouchListener()
        try {
            windowManager?.addView(bubbleView, bubbleParams)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Erro ao apresentar botão flutuante: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun updateBubbleSize(sizeDp: Int) {
        val px = dpToPx(sizeDp)
        val params = bubbleParams ?: return
        params.width = px
        params.height = px
        try {
            windowManager?.updateViewLayout(bubbleView, params)
        } catch (_: Exception) {}
        AppSettings.saveBubbleSize(this, sizeDp)
    }

    private fun setupBubbleTouchListener() {
        bubbleView?.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isClick = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                val params = bubbleParams ?: return false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = params.x
                        initialY = params.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        isClick = true
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = (event.rawX - initialTouchX).toInt()
                        val dy = (event.rawY - initialTouchY).toInt()
                        if (Math.abs(dx) > 12 || Math.abs(dy) > 12) {
                            isClick = false
                        }
                        params.x = initialX + dx
                        params.y = initialY + dy
                        try {
                            windowManager?.updateViewLayout(bubbleView, params)
                        } catch (_: Exception) {}
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isClick) {
                            toggleAssistantPanel()
                        } else {
                            // Snap suave à borda da tela
                            val screenWidth = resources.displayMetrics.widthPixels
                            params.x = if (params.x + v.width / 2 < screenWidth / 2) 20 else screenWidth - v.width - 20
                            try {
                                windowManager?.updateViewLayout(bubbleView, params)
                                AppSettings.saveBubblePosition(this@FloatingBubbleService, params.x, params.y)
                            } catch (_: Exception) {}
                        }
                        return true
                    }
                }
                return false
            }
        })
    }

    fun setBubbleVisibility(visible: Boolean) {
        bubbleView?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun setSharedContent(text: String?, imageBase64: String?) {
        if (!text.isNullOrBlank()) currentCapturedText = text
        if (!imageBase64.isNullOrBlank()) currentCapturedImageBase64 = imageBase64
        showAssistantPanel()
    }

    fun processCapturedImage(base64: String) {
        currentCapturedImageBase64 = base64
        showAssistantPanel()
    }

    private fun toggleAssistantPanel() {
        if (assistantPanelView != null) {
            hideAssistantPanel()
        } else {
            showAssistantPanel()
        }
    }

    private fun showAssistantPanel() {
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Permissão de sobreposição necessária para abrir o assistente", Toast.LENGTH_SHORT).show()
            return
        }

        hideAssistantPanel()

        val screenMetrics = resources.displayMetrics
        val screenWidth = screenMetrics.widthPixels
        val screenHeight = screenMetrics.heightPixels

        val width = (screenWidth * 0.94).toInt().coerceAtMost(dpToPx(560))
        val initialHeight = if (isExpanded) (screenHeight * 0.85).toInt() else dpToPx(490)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dpToPx(24).toFloat()
                setColor(Color.parseColor("#F2090D16")) // Glassmorphism dark
                setStroke(dpToPx(1), Color.parseColor("#2638BDF8")) // Sleek subtle border
            }
            setPadding(dpToPx(16), dpToPx(16), dpToPx(16), dpToPx(16))
            elevation = 28f
        }

        // Outside touch dismissal
        root.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_OUTSIDE) {
                hideAssistantPanel()
                true
            } else {
                false
            }
        }

        // 1. Header Row
        val header = createHeaderView()
        root.addView(header)

        // 2. Context Preview Row
        val contextView = createContextPreviewView()
        root.addView(contextView)

        // 3. Prompt Input Row (com microfone)
        val promptInput = createPromptInputView()
        root.addView(promptInput)

        // 4. Quick Action Pills
        val actionsPillRow = createActionPillsView()
        root.addView(actionsPillRow)

        // 5. Result Display Container (Scrollable)
        val resultContainer = createResultContainerView()
        root.addView(resultContainer, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        ))

        assistantPanelView = root

        panelParams = WindowManager.LayoutParams(
            width,
            initialHeight,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
            y = dpToPx(56)
        }

        try {
            windowManager?.addView(assistantPanelView, panelParams)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Falha ao abrir painel: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hideAssistantPanel() {
        stopTts()
        try {
            speechRecognizer?.stopListening()
            isListeningSpeech = false
        } catch (_: Exception) {}
        assistantPanelView?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {}
            assistantPanelView = null
        }
    }

    private fun createHeaderView(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dpToPx(12))

            // Title & Sparkle Icon
            val titleBadge = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                val iconView = ImageView(context).apply {
                    setImageResource(com.example.aiquestionassistant.R.drawable.ic_sparkle)
                    setColorFilter(Color.parseColor("#38BDF8"))
                }
                addView(iconView, LinearLayout.LayoutParams(dpToPx(18), dpToPx(18)).apply {
                    setMargins(0, 0, dpToPx(8), 0)
                })

                val title = TextView(context).apply {
                    text = "Assistente IA Global"
                    setTextColor(Color.parseColor("#F4F4F5"))
                    textSize = 15f
                    typeface = Typeface.DEFAULT_BOLD
                }
                addView(title)
            }
            addView(titleBadge, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            // Controle de tamanho do botão flutuante (- / +)
            val sizeControls = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = GradientDrawable().apply {
                    cornerRadius = dpToPx(16).toFloat()
                    setColor(Color.parseColor("#1E293B"))
                    setStroke(dpToPx(1), Color.parseColor("#334155"))
                }
                setPadding(dpToPx(6), dpToPx(2), dpToPx(6), dpToPx(2))

                val btnDecrease = ImageView(context).apply {
                    setImageResource(com.example.aiquestionassistant.R.drawable.ic_remove)
                    setColorFilter(Color.parseColor("#94A3B8"))
                    val pad = dpToPx(4)
                    setPadding(pad, pad, pad, pad)
                    setOnClickListener {
                        val current = AppSettings.load(context).bubbleSize
                        val next = (current - 6).coerceAtLeast(36)
                        updateBubbleSize(next)
                        Toast.makeText(context, "Tamanho: ${next}dp", Toast.LENGTH_SHORT).show()
                    }
                }
                addView(btnDecrease, LinearLayout.LayoutParams(dpToPx(24), dpToPx(24)))

                val sizeLabel = TextView(context).apply {
                    text = "Botão"
                    textSize = 10f
                    setTextColor(Color.parseColor("#64748B"))
                    setPadding(dpToPx(4), 0, dpToPx(4), 0)
                }
                addView(sizeLabel)

                val btnIncrease = ImageView(context).apply {
                    setImageResource(com.example.aiquestionassistant.R.drawable.ic_add)
                    setColorFilter(Color.parseColor("#38BDF8"))
                    val pad = dpToPx(4)
                    setPadding(pad, pad, pad, pad)
                    setOnClickListener {
                        val current = AppSettings.load(context).bubbleSize
                        val next = (current + 6).coerceAtMost(96)
                        updateBubbleSize(next)
                        Toast.makeText(context, "Tamanho: ${next}dp", Toast.LENGTH_SHORT).show()
                    }
                }
                addView(btnIncrease, LinearLayout.LayoutParams(dpToPx(24), dpToPx(24)))
            }
            addView(sizeControls, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, dpToPx(8), 0)
            })

            // Botão Expandir / Minimizar
            val btnExpand = ImageView(context).apply {
                setImageResource(
                    if (isExpanded) com.example.aiquestionassistant.R.drawable.ic_fullscreen_exit
                    else com.example.aiquestionassistant.R.drawable.ic_fullscreen
                )
                setColorFilter(Color.parseColor("#94A3B8"))
                background = GradientDrawable().apply {
                    cornerRadius = dpToPx(14).toFloat()
                    setColor(Color.parseColor("#1E293B"))
                }
                val pad = dpToPx(6)
                setPadding(pad, pad, pad, pad)
                setOnClickListener {
                    isExpanded = !isExpanded
                    showAssistantPanel()
                }
            }
            addView(btnExpand, LinearLayout.LayoutParams(dpToPx(28), dpToPx(28)).apply {
                setMargins(0, 0, dpToPx(6), 0)
            })

            // Botão Fechar
            val btnClose = ImageView(context).apply {
                setImageResource(com.example.aiquestionassistant.R.drawable.ic_close)
                setColorFilter(Color.parseColor("#EF4444"))
                background = GradientDrawable().apply {
                    cornerRadius = dpToPx(14).toFloat()
                    setColor(Color.parseColor("#26EF4444"))
                }
                val pad = dpToPx(6)
                setPadding(pad, pad, pad, pad)
                setOnClickListener { hideAssistantPanel() }
            }
            addView(btnClose, LinearLayout.LayoutParams(dpToPx(28), dpToPx(28)))
        }
    }

    private fun createContextPreviewView(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dpToPx(14).toFloat()
                setColor(Color.parseColor("#111827"))
                setStroke(dpToPx(1), Color.parseColor("#1F2937"))
            }
            setPadding(dpToPx(12), dpToPx(8), dpToPx(8), dpToPx(8))

            val contextText = TextView(context).apply {
                textSize = 12f

                if (!currentCapturedImageBase64.isNullOrBlank()) {
                    text = "📸 Imagem capturada (Pronta para resolver)"
                    setTextColor(Color.parseColor("#38BDF8"))
                    typeface = Typeface.DEFAULT_BOLD
                } else if (!currentCapturedText.isNullOrBlank()) {
                    val snippet = currentCapturedText!!.take(35).replace("\n", " ")
                    text = "Texto: \"$snippet...\""
                    setTextColor(Color.parseColor("#94A3B8"))
                } else {
                    text = "Capturar ou colar texto de apoio"
                    setTextColor(Color.parseColor("#94A3B8"))
                }
            }
            addView(contextText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            // Ação: Capturar Ecrã (com ícone de câmera)
            val btnCapture = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = GradientDrawable().apply {
                    cornerRadius = dpToPx(10).toFloat()
                    setColor(Color.parseColor("#1E293B"))
                    setStroke(dpToPx(1), Color.parseColor("#38BDF8"))
                }
                setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))

                val camIcon = ImageView(context).apply {
                    setImageResource(com.example.aiquestionassistant.R.drawable.ic_camera)
                    setColorFilter(Color.parseColor("#38BDF8"))
                }
                addView(camIcon, LinearLayout.LayoutParams(dpToPx(14), dpToPx(14)).apply {
                    setMargins(0, 0, dpToPx(4), 0)
                })

                val label = TextView(context).apply {
                    text = "Ecrã"
                    setTextColor(Color.parseColor("#38BDF8"))
                    textSize = 11f
                    typeface = Typeface.DEFAULT_BOLD
                }
                addView(label)

                setOnClickListener {
                    hideAssistantPanel()
                    val intent = Intent(this@FloatingBubbleService, ScreenCaptureActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    startActivity(intent)
                }
            }
            addView(btnCapture, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, dpToPx(6), 0)
            })

            // Ação: Colar
            val btnPaste = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = GradientDrawable().apply {
                    cornerRadius = dpToPx(10).toFloat()
                    setColor(Color.parseColor("#1E293B"))
                    setStroke(dpToPx(1), Color.parseColor("#A855F7"))
                }
                setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))

                val pasteIcon = ImageView(context).apply {
                    setImageResource(com.example.aiquestionassistant.R.drawable.ic_content_paste)
                    setColorFilter(Color.parseColor("#A855F7"))
                }
                addView(pasteIcon, LinearLayout.LayoutParams(dpToPx(14), dpToPx(14)).apply {
                    setMargins(0, 0, dpToPx(4), 0)
                })

                val label = TextView(context).apply {
                    text = "Colar"
                    setTextColor(Color.parseColor("#A855F7"))
                    textSize = 11f
                    typeface = Typeface.DEFAULT_BOLD
                }
                addView(label)

                setOnClickListener {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = clipboard.primaryClip?.getItemAt(0)?.text?.toString()
                    if (!clip.isNullOrBlank()) {
                        currentCapturedText = clip
                        showAssistantPanel()
                        Toast.makeText(context, "Texto colado!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Área de transferência vazia", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            addView(btnPaste)

            // Se houver contexto, botão de limpar
            if (!currentCapturedImageBase64.isNullOrBlank() || !currentCapturedText.isNullOrBlank()) {
                val btnClear = ImageView(context).apply {
                    setImageResource(com.example.aiquestionassistant.R.drawable.ic_close)
                    setColorFilter(Color.parseColor("#EF4444"))
                    val pad = dpToPx(4)
                    setPadding(pad, pad, pad, pad)
                    setOnClickListener {
                        currentCapturedImageBase64 = null
                        currentCapturedText = null
                        showAssistantPanel()
                    }
                }
                addView(btnClear, LinearLayout.LayoutParams(dpToPx(22), dpToPx(22)).apply {
                    setMargins(dpToPx(4), 0, 0, 0)
                })
            }
        }
    }

    private var promptEditText: EditText? = null

    private fun createPromptInputView(): LinearLayout {
        return LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dpToPx(10), 0, dpToPx(10))

            promptEditText = EditText(context).apply {
                hint = "Faça uma pergunta, instrução ou tema..."
                setHintTextColor(Color.parseColor("#64748B"))
                setTextColor(Color.WHITE)
                textSize = 13f
                background = GradientDrawable().apply {
                    cornerRadius = dpToPx(14).toFloat()
                    setColor(Color.parseColor("#111827"))
                    setStroke(dpToPx(1), Color.parseColor("#1F2937"))
                }
                setPadding(dpToPx(14), dpToPx(10), dpToPx(14), dpToPx(10))
                maxLines = 2
                imeOptions = android.view.inputmethod.EditorInfo.IME_ACTION_SEND
                inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_SEND) {
                        val text = text?.toString()?.trim() ?: ""
                        if (text.isNotBlank()) {
                            triggerAssistantAction(AssistantActionType.ASK)
                        } else if (!currentCapturedImageBase64.isNullOrBlank()) {
                            triggerAssistantAction(AssistantActionType.SOLVE)
                        }
                        true
                    } else false
                }
            }
            addView(promptEditText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            // Botão Microfone / Ditar por voz (com ícone vetorial)
            val btnMic = ImageView(context).apply {
                setImageResource(com.example.aiquestionassistant.R.drawable.ic_mic)
                setColorFilter(Color.parseColor("#38BDF8"))
                background = GradientDrawable().apply {
                    cornerRadius = dpToPx(14).toFloat()
                    setColor(Color.parseColor("#1E293B"))
                    setStroke(dpToPx(1), Color.parseColor("#334155"))
                }
                val pad = dpToPx(8)
                setPadding(pad, pad, pad, pad)
                setOnClickListener { startVoiceRecognition() }
            }
            addView(btnMic, LinearLayout.LayoutParams(dpToPx(38), dpToPx(38)).apply {
                setMargins(dpToPx(8), 0, 0, 0)
            })

            // Botão Enviar Mensagem (Seta)
            val btnSend = ImageView(context).apply {
                setImageResource(com.example.aiquestionassistant.R.drawable.ic_send)
                setColorFilter(Color.WHITE)
                background = GradientDrawable().apply {
                    cornerRadius = dpToPx(14).toFloat()
                    setColor(Color.parseColor("#1D51D8"))
                }
                val pad = dpToPx(8)
                setPadding(pad, pad, pad, pad)
                setOnClickListener {
                    val text = promptEditText?.text?.toString()?.trim() ?: ""
                    if (text.isNotBlank()) {
                        triggerAssistantAction(AssistantActionType.ASK)
                    } else if (!currentCapturedImageBase64.isNullOrBlank()) {
                        triggerAssistantAction(AssistantActionType.SOLVE)
                    } else {
                        Toast.makeText(context, "Digite uma pergunta para enviar", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            addView(btnSend, LinearLayout.LayoutParams(dpToPx(38), dpToPx(38)).apply {
                setMargins(dpToPx(6), 0, 0, 0)
            })
        }
    }

    private fun getActionDrawableRes(action: AssistantActionType): Int {
        return when (action) {
            AssistantActionType.SUMMARIZE -> com.example.aiquestionassistant.R.drawable.ic_action_summarize
            AssistantActionType.EXPLAIN -> com.example.aiquestionassistant.R.drawable.ic_action_explain
            AssistantActionType.DIAGRAM -> com.example.aiquestionassistant.R.drawable.ic_action_diagram
            AssistantActionType.IMAGE -> com.example.aiquestionassistant.R.drawable.ic_action_image
            AssistantActionType.TRANSLATE -> com.example.aiquestionassistant.R.drawable.ic_action_translate
            AssistantActionType.FLASHCARDS -> com.example.aiquestionassistant.R.drawable.ic_action_cards
            AssistantActionType.SOLVE -> com.example.aiquestionassistant.R.drawable.ic_action_solve
            AssistantActionType.ASK -> com.example.aiquestionassistant.R.drawable.ic_sparkle
        }
    }

    private fun createActionPillsView(): HorizontalScrollView {
        val scroll = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            setPadding(0, 0, 0, dpToPx(10))
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
        }

        val actions = listOf(
            AssistantActionType.SOLVE to "#6366F1",
            AssistantActionType.EXPLAIN to "#3B82F6",
            AssistantActionType.SUMMARIZE to "#06B6D4",
            AssistantActionType.DIAGRAM to "#8B5CF6",
            AssistantActionType.TRANSLATE to "#10B981",
            AssistantActionType.FLASHCARDS to "#F59E0B",
            AssistantActionType.IMAGE to "#EC4899"
        )

        for ((action, hexColor) in actions) {
            val pill = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = GradientDrawable().apply {
                    cornerRadius = dpToPx(16).toFloat()
                    setColor(Color.parseColor("#1E293B"))
                    setStroke(dpToPx(1), Color.parseColor(hexColor))
                }
                setPadding(dpToPx(10), dpToPx(6), dpToPx(12), dpToPx(6))
                val margin = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    setMargins(0, 0, dpToPx(8), 0)
                }
                layoutParams = margin

                val iconView = ImageView(context).apply {
                    setImageResource(getActionDrawableRes(action))
                    setColorFilter(Color.parseColor(hexColor))
                }
                addView(iconView, LinearLayout.LayoutParams(dpToPx(14), dpToPx(14)).apply {
                    setMargins(0, 0, dpToPx(6), 0)
                })

                val label = TextView(context).apply {
                    text = action.label
                    setTextColor(Color.WHITE)
                    textSize = 12f
                    typeface = Typeface.DEFAULT_BOLD
                }
                addView(label)

                setOnClickListener {
                    triggerAssistantAction(action)
                }
            }
            row.addView(pill)
        }

        scroll.addView(row)
        return scroll
    }

    private var resultContainerView: FrameLayout? = null

    private fun createResultContainerView(): FrameLayout {
        val container = FrameLayout(this).apply {
            background = GradientDrawable().apply {
                cornerRadius = dpToPx(18).toFloat()
                setColor(Color.parseColor("#111827")) // Modern container
                setStroke(dpToPx(1), Color.parseColor("#1F2937"))
            }
            setPadding(dpToPx(14), dpToPx(12), dpToPx(14), dpToPx(12))
        }

        resultContainerView = container

        // Se já tivermos um último resultado, exibe-o
        lastResult?.let {
            renderResultInContainer(it)
        } ?: run {
            renderWelcomeInContainer()
        }

        return container
    }

    private fun renderWelcomeInContainer() {
        val root = resultContainerView ?: return
        root.removeAllViews()

        val welcomeLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dpToPx(12), dpToPx(20), dpToPx(12), dpToPx(20))

            val sparkIcon = ImageView(context).apply {
                setImageResource(com.example.aiquestionassistant.R.drawable.ic_sparkle)
                setColorFilter(Color.parseColor("#38BDF8"))
            }
            addView(sparkIcon, LinearLayout.LayoutParams(dpToPx(32), dpToPx(32)).apply {
                setMargins(0, 0, 0, dpToPx(10))
            })

            val heading = TextView(context).apply {
                text = "Como posso ajudar agora?"
                setTextColor(Color.WHITE)
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, 0, 0, dpToPx(6))
            }
            addView(heading)

            val desc = TextView(context).apply {
                text = "Selecione uma ação acima ou formule uma pergunta personalizada."
                setTextColor(Color.parseColor("#64748B"))
                textSize = 12f
                gravity = Gravity.CENTER
                setPadding(0, 0, 0, dpToPx(16))
            }
            addView(desc)

            val features = listOf(
                "Resumir documentos, páginas e artigos",
                "Gerar diagramas e mapas mentais conceituais",
                "Criar ilustrações temáticas com IA",
                "Explicar conceitos técnicos detalhadamente"
            )

            for (feat in features) {
                val row = LinearLayout(context).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    setPadding(0, dpToPx(2), 0, dpToPx(2))

                    val dot = ImageView(context).apply {
                        setImageResource(com.example.aiquestionassistant.R.drawable.ic_sparkle)
                        setColorFilter(Color.parseColor("#38BDF8"))
                    }
                    addView(dot, LinearLayout.LayoutParams(dpToPx(8), dpToPx(8)).apply {
                        setMargins(0, 0, dpToPx(8), 0)
                    })

                    val label = TextView(context).apply {
                        text = feat
                        setTextColor(Color.parseColor("#94A3B8"))
                        textSize = 12f
                    }
                    addView(label)
                }
                addView(row)
            }
        }
        root.addView(welcomeLayout)
    }

    private fun renderLoadingInContainer(action: AssistantActionType) {
        val root = resultContainerView ?: return
        root.removeAllViews()

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dpToPx(16), dpToPx(32), dpToPx(16), dpToPx(32))

            val pBar = ProgressBar(context).apply {
                indeterminateTintList = ColorStateList.valueOf(Color.parseColor("#38BDF8"))
            }
            addView(pBar, LinearLayout.LayoutParams(dpToPx(36), dpToPx(36)))

            val label = TextView(context).apply {
                text = "A processar ${action.label} com IA..."
                setTextColor(Color.WHITE)
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
                setPadding(0, dpToPx(12), 0, dpToPx(12))
            }
            addView(label)

            // Botão Cancelar
            val btnCancel = TextView(context).apply {
                text = "Cancelar"
                setTextColor(Color.parseColor("#EF4444"))
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                background = GradientDrawable().apply {
                    cornerRadius = dpToPx(12).toFloat()
                    setColor(Color.parseColor("#26EF4444"))
                    setStroke(dpToPx(1), Color.parseColor("#EF4444"))
                }
                setPadding(dpToPx(16), dpToPx(6), dpToPx(16), dpToPx(6))
                setOnClickListener {
                    currentExecutionJob?.cancel()
                    renderWelcomeInContainer()
                    Toast.makeText(context, "Operação cancelada", Toast.LENGTH_SHORT).show()
                }
            }
            addView(btnCancel)
        }

        root.addView(layout, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
    }

    private fun renderResultInContainer(result: AssistantResult) {
        val root = resultContainerView ?: return
        root.removeAllViews()

        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        // Top Actions Bar (TTS, Copy, Share, Save)
        val toolbar = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, 0, 0, dpToPx(10))

            val titleBadge = TextView(context).apply {
                text = result.title.ifBlank { result.action.label }
                setTextColor(Color.parseColor("#38BDF8"))
                textSize = 13f
                typeface = Typeface.DEFAULT_BOLD
            }
            addView(titleBadge, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            // Botão TTS Ouvir
            val btnTts = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = GradientDrawable().apply {
                    cornerRadius = dpToPx(10).toFloat()
                    setColor(Color.parseColor("#1E293B"))
                    setStroke(dpToPx(1), Color.parseColor("#F59E0B"))
                }
                setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))

                val ttsIcon = ImageView(context).apply {
                    setImageResource(
                        if (isTtsSpeaking) com.example.aiquestionassistant.R.drawable.ic_stop
                        else com.example.aiquestionassistant.R.drawable.ic_volume_up
                    )
                    setColorFilter(Color.parseColor("#F59E0B"))
                }
                addView(ttsIcon, LinearLayout.LayoutParams(dpToPx(14), dpToPx(14)).apply {
                    setMargins(0, 0, dpToPx(4), 0)
                })

                val label = TextView(context).apply {
                    text = if (isTtsSpeaking) "Parar" else "Ouvir"
                    setTextColor(Color.parseColor("#F59E0B"))
                    textSize = 11f
                    typeface = Typeface.DEFAULT_BOLD
                }
                addView(label)

                setOnClickListener { toggleTts(result.resultText) }
            }
            addView(btnTts, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, dpToPx(6), 0)
            })

            // Botão Copiar
            val btnCopy = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = GradientDrawable().apply {
                    cornerRadius = dpToPx(10).toFloat()
                    setColor(Color.parseColor("#1E293B"))
                    setStroke(dpToPx(1), Color.parseColor("#38BDF8"))
                }
                setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))

                val copyIcon = ImageView(context).apply {
                    setImageResource(com.example.aiquestionassistant.R.drawable.ic_content_copy)
                    setColorFilter(Color.parseColor("#38BDF8"))
                }
                addView(copyIcon, LinearLayout.LayoutParams(dpToPx(14), dpToPx(14)).apply {
                    setMargins(0, 0, dpToPx(4), 0)
                })

                val label = TextView(context).apply {
                    text = "Copiar"
                    setTextColor(Color.parseColor("#38BDF8"))
                    textSize = 11f
                    typeface = Typeface.DEFAULT_BOLD
                }
                addView(label)

                setOnClickListener {
                    val fullText = buildString {
                        append(result.title).append("\n\n").append(result.resultText)
                        if (!result.diagramCode.isNullOrBlank()) {
                            append("\n\n```mermaid\n").append(result.diagramCode).append("\n```")
                        }
                    }
                    copyToClipboard(fullText)
                    Toast.makeText(context, "Copiado para a área de transferência!", Toast.LENGTH_SHORT).show()
                }
            }
            addView(btnCopy, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(0, 0, dpToPx(6), 0)
            })

            // Botão Partilhar
            val btnShare = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = GradientDrawable().apply {
                    cornerRadius = dpToPx(10).toFloat()
                    setColor(Color.parseColor("#1E293B"))
                    setStroke(dpToPx(1), Color.parseColor("#10B981"))
                }
                setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))

                val shareIcon = ImageView(context).apply {
                    setImageResource(com.example.aiquestionassistant.R.drawable.ic_share)
                    setColorFilter(Color.parseColor("#10B981"))
                }
                addView(shareIcon, LinearLayout.LayoutParams(dpToPx(14), dpToPx(14)).apply {
                    setMargins(0, 0, dpToPx(4), 0)
                })

                val label = TextView(context).apply {
                    text = "Partilhar"
                    setTextColor(Color.parseColor("#10B981"))
                    textSize = 11f
                    typeface = Typeface.DEFAULT_BOLD
                }
                addView(label)

                setOnClickListener { shareResult(result) }
            }
            addView(btnShare)
        }
        mainLayout.addView(toolbar)

        // Conteúdo com Scroll
        val scroll = ScrollView(this).apply {
            val contentLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL

                // Se houver imagem gerada
                if (!result.imageUrl.isNullOrBlank()) {
                    val imageBanner = LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(0, 0, 0, dpToPx(8))

                        val imgIcon = ImageView(context).apply {
                            setImageResource(com.example.aiquestionassistant.R.drawable.ic_action_image)
                            setColorFilter(Color.parseColor("#EC4899"))
                        }
                        addView(imgIcon, LinearLayout.LayoutParams(dpToPx(14), dpToPx(14)).apply {
                            setMargins(0, 0, dpToPx(6), 0)
                        })

                        val bannerLabel = TextView(context).apply {
                            text = "Imagem gerada pela IA:"
                            setTextColor(Color.parseColor("#EC4899"))
                            textSize = 12f
                            typeface = Typeface.DEFAULT_BOLD
                        }
                        addView(bannerLabel)
                    }
                    addView(imageBanner)

                    val btnOpenImage = LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        background = GradientDrawable().apply {
                            cornerRadius = dpToPx(10).toFloat()
                            setColor(Color.parseColor("#EC4899"))
                        }
                        setPadding(dpToPx(12), dpToPx(8), dpToPx(12), dpToPx(8))

                        val label = TextView(context).apply {
                            text = "Abrir Imagem em Alta Resolução"
                            setTextColor(Color.WHITE)
                            textSize = 12f
                            typeface = Typeface.DEFAULT_BOLD
                        }
                        addView(label)

                        val extIcon = ImageView(context).apply {
                            setImageResource(com.example.aiquestionassistant.R.drawable.ic_open_in_new)
                            setColorFilter(Color.WHITE)
                        }
                        addView(extIcon, LinearLayout.LayoutParams(dpToPx(14), dpToPx(14)).apply {
                            setMargins(dpToPx(6), 0, 0, 0)
                        })

                        setOnClickListener {
                            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(result.imageUrl)).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            startActivity(browserIntent)
                        }
                    }
                    addView(btnOpenImage)
                }

                // Se houver Diagrama Mermaid
                if (!result.diagramCode.isNullOrBlank()) {
                    val diagramHeader = LinearLayout(context).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(0, dpToPx(10), 0, dpToPx(6))

                        val dIcon = ImageView(context).apply {
                            setImageResource(com.example.aiquestionassistant.R.drawable.ic_action_diagram)
                            setColorFilter(Color.parseColor("#8B5CF6"))
                        }
                        addView(dIcon, LinearLayout.LayoutParams(dpToPx(14), dpToPx(14)).apply {
                            setMargins(0, 0, dpToPx(6), 0)
                        })

                        val dLabel = TextView(context).apply {
                            text = "Diagrama Mermaid Gerado:"
                            setTextColor(Color.parseColor("#8B5CF6"))
                            textSize = 12f
                            typeface = Typeface.DEFAULT_BOLD
                        }
                        addView(dLabel)
                    }
                    addView(diagramHeader)

                    val diagramBox = TextView(context).apply {
                        text = result.diagramCode
                        setTextColor(Color.parseColor("#A7F3D0"))
                        textSize = 11f
                        typeface = Typeface.MONOSPACE
                        background = GradientDrawable().apply {
                            cornerRadius = dpToPx(10).toFloat()
                            setColor(Color.parseColor("#090D16"))
                            setStroke(dpToPx(1), Color.parseColor("#1F2937"))
                        }
                        setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10))
                    }
                    addView(diagramBox)
                }

                // Texto principal formatado
                val bodyText = TextView(context).apply {
                    text = result.resultText
                    setTextColor(Color.parseColor("#F4F4F5"))
                    textSize = 13f
                    setLineSpacing(5f, 1.15f)
                    setPadding(0, dpToPx(8), 0, 0)
                }
                addView(bodyText)
            }
            addView(contentLayout)
        }
        mainLayout.addView(scroll, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT))

        root.addView(mainLayout)
    }

    private fun triggerAssistantAction(action: AssistantActionType) {
        val settings = AppSettings.load(this)
        val prompt = promptEditText?.text?.toString()?.trim()

        renderLoadingInContainer(action)

        currentExecutionJob = serviceScope.launch {
            val result = aiRepository.executeAction(
                settings = settings,
                action = action,
                content = currentCapturedText,
                imageBase64 = currentCapturedImageBase64,
                prompt = prompt,
                options = AssistantActionOptions(targetLanguage = settings.preferredLanguage)
            )

            lastResult = result

            if (result.isSuccess) {
                historyRepository.saveItem(result)
                renderResultInContainer(result)

                if (settings.autoCopy && result.action == AssistantActionType.SOLVE) {
                    copyToClipboard(result.resultText)
                    Toast.makeText(this@FloatingBubbleService, "Gabarito copiado!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this@FloatingBubbleService, result.error ?: "Falha ao processar", Toast.LENGTH_LONG).show()
                renderWelcomeInContainer()
            }
        }
    }

    private fun toggleTts(textToSpeak: String) {
        if (isTtsSpeaking) {
            stopTts()
        } else {
            val clean = textToSpeak.replace(Regex("""[#*`_]"""), "").trim()
            if (clean.isNotBlank()) {
                tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, "tts_id")
                isTtsSpeaking = true
                lastResult?.let { renderResultInContainer(it) }
            }
        }
    }

    private fun stopTts() {
        if (tts?.isSpeaking == true) {
            tts?.stop()
        }
        isTtsSpeaking = false
        lastResult?.let { renderResultInContainer(it) }
    }

    private fun startVoiceRecognition() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Permissão de microfone necessária. Abra a aplicação para conceder.", Toast.LENGTH_LONG).show()
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra("request_audio_permission", true)
            }
            startActivity(intent)
            return
        }

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            Toast.makeText(this, "Reconhecimento de voz não suportado neste aparelho", Toast.LENGTH_SHORT).show()
            return
        }

        if (isListeningSpeech) {
            try {
                speechRecognizer?.stopListening()
            } catch (_: Exception) {}
            isListeningSpeech = false
            Toast.makeText(this, "Gravação de voz cancelada", Toast.LENGTH_SHORT).show()
            return
        }

        basePromptTextBeforeSpeech = promptEditText?.text?.toString() ?: ""

        try {
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListeningSpeech = true
                        Toast.makeText(this@FloatingBubbleService, "Pode falar agora...", Toast.LENGTH_SHORT).show()
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        isListeningSpeech = false
                    }
                    override fun onError(error: Int) {
                        isListeningSpeech = false
                        val errorMsg = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Falha na captura de áudio do microfone"
                            SpeechRecognizer.ERROR_CLIENT -> "Erro no cliente de voz do sistema"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissão de microfone necessária"
                            SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Sem ligação à internet para reconhecimento de voz"
                            SpeechRecognizer.ERROR_NO_MATCH -> "Nenhuma fala detectada. Fale novamente"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Microfone ocupado por outra aplicação"
                            SpeechRecognizer.ERROR_SERVER -> "Erro nos servidores de reconhecimento de voz"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Tempo esgotado sem som detectado"
                            else -> "Erro no reconhecimento de voz ($error)"
                        }
                        Toast.makeText(this@FloatingBubbleService, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        if (!matches.isNullOrEmpty()) {
                            val recognized = matches[0].trim()
                            val combined = if (basePromptTextBeforeSpeech.isNotBlank()) {
                                "$basePromptTextBeforeSpeech $recognized"
                            } else {
                                recognized
                            }
                            promptEditText?.setText(combined)
                            promptEditText?.setSelection(combined.length)
                            Toast.makeText(this@FloatingBubbleService, "Texto reconhecido!", Toast.LENGTH_SHORT).show()
                        }
                        isListeningSpeech = false
                    }
                    override fun onPartialResults(partialResults: Bundle?) {
                        val partialMatches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = partialMatches?.firstOrNull()?.trim()
                        if (!partial.isNullOrBlank()) {
                            val combined = if (basePromptTextBeforeSpeech.isNotBlank()) {
                                "$basePromptTextBeforeSpeech $partial"
                            } else {
                                partial
                            }
                            promptEditText?.setText(combined)
                            promptEditText?.setSelection(combined.length)
                        }
                    }
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val lang = Locale.getDefault().toLanguageTag().ifBlank { "pt-BR" }
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, lang)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            speechRecognizer?.startListening(intent)
            isListeningSpeech = true
        } catch (e: Exception) {
            isListeningSpeech = false
            Toast.makeText(this, "Erro ao iniciar microfone: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareResult(result: AssistantResult) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, result.title)
            val fullText = buildString {
                append(result.title).append("\n\n").append(result.resultText)
                if (!result.imageUrl.isNullOrBlank()) {
                    append("\n\nImagem gerada: ").append(result.imageUrl)
                }
                if (!result.diagramCode.isNullOrBlank()) {
                    append("\n\nDiagrama Mermaid:\n```mermaid\n").append(result.diagramCode).append("\n```")
                }
            }
            putExtra(Intent.EXTRA_TEXT, fullText)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(Intent.createChooser(shareIntent, "Partilhar Análise IA").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("AI Assistant", text)
        clipboard.setPrimaryClip(clip)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        isRunning = false
        stopTts()
        try {
            tts?.shutdown()
        } catch (_: Exception) {}
        try {
            speechRecognizer?.destroy()
        } catch (_: Exception) {}
        hideAssistantPanel()
        bubbleView?.let {
            try {
                windowManager?.removeView(it)
            } catch (_: Exception) {}
            bubbleView = null
        }
    }
}
