package com.example.aiquestionassistant.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.example.aiquestionassistant.MainActivity
import com.example.aiquestionassistant.capture.ScreenCaptureActivity
import com.example.aiquestionassistant.data.AiRepository
import com.example.aiquestionassistant.data.AppSettings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class FloatingBubbleService : Service() {

    companion object {
        var instance: FloatingBubbleService? = null
        var isRunning = false
        private const val CHANNEL_ID = "ai_assistant_service_channel"
        private const val NOTIFICATION_ID = 2001
    }

    private var windowManager: WindowManager? = null
    private var bubbleView: FrameLayout? = null
    private var menuView: LinearLayout? = null
    private var resultCardView: LinearLayout? = null

    private var bubbleParams: WindowManager.LayoutParams? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private val aiRepository = AiRepository()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        isRunning = true
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager

        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())

        createFloatingBubble()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Assistente IA Flutuante",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificação para manter o assistente flutuante ativo sobre outras apps"
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
            Notification.Builder(this)
        }

        return builder
            .setContentTitle("Assistente IA de Provas Ativo")
            .setContentText("Toque na bolha flutuante no ecrã para analisar questões")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createFloatingBubble() {
        bubbleView = FrameLayout(this).apply {
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                colors = intArrayOf(Color.parseColor("#38BDF8"), Color.parseColor("#6366F1"))
                gradientType = GradientDrawable.LINEAR_GRADIENT
                setStroke(4, Color.parseColor("#FFFFFF"))
            }
            background = bg
            elevation = 16f

            // Inner icon
            val icon = TextView(context).apply {
                text = "⚡"
                textSize = 24f
                gravity = Gravity.CENTER
            }
            addView(icon, FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            ))
        }

        val size = dpToPx(60)
        bubbleParams = WindowManager.LayoutParams(
            size, size,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 20
            y = 300
        }

        setupBubbleTouchListener()
        windowManager?.addView(bubbleView, bubbleParams)
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
                        if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                            isClick = false
                        }
                        params.x = initialX + dx
                        params.y = initialY + dy
                        windowManager?.updateViewLayout(bubbleView, params)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        if (isClick) {
                            toggleQuickMenu()
                        } else {
                            // Snap to nearest edge
                            val screenWidth = resources.displayMetrics.widthPixels
                            params.x = if (params.x + v.width / 2 < screenWidth / 2) 20 else screenWidth - v.width - 20
                            windowManager?.updateViewLayout(bubbleView, params)
                        }
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun toggleQuickMenu() {
        if (menuView != null) {
            hideQuickMenu()
            return
        }

        val bParams = bubbleParams ?: return
        menuView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dpToPx(16).toFloat()
                setColor(Color.parseColor("#E60F172A")) // Modern dark glass
                setStroke(2, Color.parseColor("#334155"))
            }
            setPadding(dpToPx(12), dpToPx(12), dpToPx(12), dpToPx(12))

            // Action: Capturar Ecrã
            val btnSnip = createMenuItem("📸  Capturar Ecrã") {
                hideQuickMenu()
                val intent = Intent(this@FloatingBubbleService, ScreenCaptureActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            }
            addView(btnSnip)

            // Action: Abrir App
            val btnSettings = createMenuItem("⚙️  Configurações") {
                hideQuickMenu()
                val intent = Intent(this@FloatingBubbleService, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            }
            addView(btnSettings)

            // Action: Fechar Assistente
            val btnClose = createMenuItem("✕  Parar Assistente") {
                hideQuickMenu()
                stopSelf()
            }
            addView(btnClose)
        }

        val menuParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = bParams.x + dpToPx(70)
            y = bParams.y
        }

        windowManager?.addView(menuView, menuParams)
    }

    private fun createMenuItem(label: String, onClick: () -> Unit): TextView {
        return TextView(this).apply {
            text = label
            setTextColor(Color.WHITE)
            textSize = 14f
            setTypeface(null, Typeface.BOLD)
            setPadding(dpToPx(16), dpToPx(10), dpToPx(16), dpToPx(10))
            setOnClickListener { onClick() }
        }
    }

    private fun hideQuickMenu() {
        menuView?.let {
            windowManager?.removeView(it)
            menuView = null
        }
    }

    fun setBubbleVisibility(visible: Boolean) {
        bubbleView?.visibility = if (visible) View.VISIBLE else View.GONE
    }

    fun processCapturedImage(base64: String) {
        showLoadingCard()
        val settings = AppSettings.load(this)

        serviceScope.launch {
            val result = aiRepository.solve(settings, imageBase64 = base64)
            hideResultCard()

            if (result.isSuccess) {
                showResultCard(
                    option = result.correctOption ?: "Análise Concluída",
                    explanation = result.explanation
                )
                if (settings.autoCopy && !result.correctOption.isNullOrBlank()) {
                    copyToClipboard(result.correctOption)
                    Toast.makeText(this@FloatingBubbleService, "Gabarito copiado!", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(
                    this@FloatingBubbleService,
                    result.error ?: "Falha ao analisar",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showLoadingCard() {
        hideResultCard()
        resultCardView = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dpToPx(20).toFloat()
                setColor(Color.parseColor("#F20F172A"))
                setStroke(2, Color.parseColor("#38BDF8"))
            }
            setPadding(dpToPx(16), dpToPx(12), dpToPx(20), dpToPx(12))

            val pBar = ProgressBar(context).apply {
                indeterminateTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#38BDF8"))
            }
            addView(pBar, LinearLayout.LayoutParams(dpToPx(28), dpToPx(28)))

            val label = TextView(context).apply {
                text = "  A resolver com IA..."
                setTextColor(Color.WHITE)
                textSize = 14f
                setTypeface(null, Typeface.BOLD)
            }
            addView(label)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
            y = dpToPx(80)
        }

        windowManager?.addView(resultCardView, params)
    }

    private fun showResultCard(option: String, explanation: String) {
        hideResultCard()

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = GradientDrawable().apply {
                cornerRadius = dpToPx(20).toFloat()
                setColor(Color.parseColor("#F50F172A")) // Sleek glass dark
                setStroke(2, Color.parseColor("#38BDF8"))
            }
            setPadding(dpToPx(18), dpToPx(16), dpToPx(18), dpToPx(16))

            // Header row with Option badge and Close button
            val headerRow = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL

                val badge = TextView(context).apply {
                    text = "  $option  "
                    setTextColor(Color.parseColor("#10B981")) // Emerald green
                    textSize = 16f
                    setTypeface(null, Typeface.BOLD)
                    background = GradientDrawable().apply {
                        cornerRadius = dpToPx(8).toFloat()
                        setColor(Color.parseColor("#1A10B981"))
                        setStroke(2, Color.parseColor("#10B981"))
                    }
                    setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
                }
                addView(badge, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

                val copyBtn = TextView(context).apply {
                    text = "Copiar"
                    setTextColor(Color.parseColor("#38BDF8"))
                    textSize = 13f
                    setTypeface(null, Typeface.BOLD)
                    setPadding(dpToPx(10), dpToPx(4), dpToPx(10), dpToPx(4))
                    setOnClickListener {
                        copyToClipboard("$option\n\n$explanation")
                        Toast.makeText(context, "Copiado!", Toast.LENGTH_SHORT).show()
                    }
                }
                addView(copyBtn)

                val closeBtn = TextView(context).apply {
                    text = " ✕ "
                    setTextColor(Color.parseColor("#94A3B8"))
                    textSize = 16f
                    setPadding(dpToPx(8), dpToPx(4), dpToPx(8), dpToPx(4))
                    setOnClickListener { hideResultCard() }
                }
                addView(closeBtn)
            }
            addView(headerRow)

            // Scrollable explanation
            val scrollView = ScrollView(context).apply {
                val expText = TextView(context).apply {
                    text = explanation
                    setTextColor(Color.parseColor("#CBD5E1"))
                    textSize = 13f
                    setLineSpacing(6f, 1.1f)
                    setPadding(0, dpToPx(10), 0, 0)
                }
                addView(expText)
            }
            val maxH = dpToPx(280)
            addView(scrollView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, maxH))
        }

        val screenWidth = resources.displayMetrics.widthPixels
        val cardWidth = (screenWidth * 0.90).toInt()

        val params = WindowManager.LayoutParams(
            cardWidth,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
            y = dpToPx(80)
        }

        resultCardView = card
        windowManager?.addView(resultCardView, params)
    }

    private fun hideResultCard() {
        resultCardView?.let {
            windowManager?.removeView(it)
            resultCardView = null
        }
    }

    private fun copyToClipboard(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("AI Answer", text)
        clipboard.setPrimaryClip(clip)
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        isRunning = false
        hideQuickMenu()
        hideResultCard()
        bubbleView?.let {
            windowManager?.removeView(it)
            bubbleView = null
        }
    }
}
