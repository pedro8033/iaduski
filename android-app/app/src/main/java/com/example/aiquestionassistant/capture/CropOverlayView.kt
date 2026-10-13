package com.example.aiquestionassistant.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View

class CropOverlayView(
    context: Context,
    private val bitmap: Bitmap,
    private val onCropSelected: (RectF) -> Unit,
    private val onCancel: () -> Unit
) : View(context) {

    private val dimPaint = Paint().apply {
        color = Color.parseColor("#7A000000") // 48% black dim for contrast
    }

    private val borderPaint = Paint().apply {
        color = Color.parseColor("#38BDF8") // Cyan accent
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(2.5f)
        isAntiAlias = true
    }

    private val cornerPaint = Paint().apply {
        color = Color.parseColor("#38BDF8")
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(4.5f)
        strokeCap = Paint.Cap.ROUND
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = dpToPx(13.5f)
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private val badgeBackgroundPaint = Paint().apply {
        color = Color.parseColor("#E6090D16") // Deep glass dark
        isAntiAlias = true
    }

    private val badgeBorderPaint = Paint().apply {
        color = Color.parseColor("#3338BDF8") // Subtle cyan stroke
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(1f)
        isAntiAlias = true
    }

    private var startX = 0f
    private var startY = 0f
    private var currentX = 0f
    private var currentY = 0f
    private var isDragging = false

    private val srcRect = Rect()
    private val viewRect = RectF()

    init {
        isFocusable = true
        isClickable = true
    }

    private fun dpToPx(dp: Float): Float {
        return dp * resources.displayMetrics.density
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val viewW = width.toFloat()
        val viewH = height.toFloat()
        if (viewW <= 0f || viewH <= 0f) return

        viewRect.set(0f, 0f, viewW, viewH)

        // 1. Draw the captured frozen screen bitmap
        canvas.drawBitmap(bitmap, null, viewRect, null)

        // 2. Draw dim overlay over the entire screen
        canvas.drawRect(viewRect, dimPaint)

        // 3. If dragging a selection rectangle, reveal the original crisp bitmap
        if (isDragging) {
            val rect = getSelectionRect()
            if (rect.width() > 10f && rect.height() > 10f) {
                val scaleX = bitmap.width.toFloat() / viewW
                val scaleY = bitmap.height.toFloat() / viewH

                srcRect.set(
                    (rect.left * scaleX).toInt().coerceIn(0, bitmap.width),
                    (rect.top * scaleY).toInt().coerceIn(0, bitmap.height),
                    (rect.right * scaleX).toInt().coerceIn(0, bitmap.width),
                    (rect.bottom * scaleY).toInt().coerceIn(0, bitmap.height)
                )

                // Draw highlighted portion
                canvas.drawBitmap(bitmap, srcRect, rect, null)

                // Draw cyan border
                canvas.drawRect(rect, borderPaint)

                // Draw corner handles
                val cornerLen = minOf(dpToPx(18f), minOf(rect.width(), rect.height()) / 3.5f)
                // Top-left
                canvas.drawLine(rect.left, rect.top, rect.left + cornerLen, rect.top, cornerPaint)
                canvas.drawLine(rect.left, rect.top, rect.left, rect.top + cornerLen, cornerPaint)
                // Top-right
                canvas.drawLine(rect.right, rect.top, rect.right - cornerLen, rect.top, cornerPaint)
                canvas.drawLine(rect.right, rect.top, rect.right, rect.top + cornerLen, cornerPaint)
                // Bottom-left
                canvas.drawLine(rect.left, rect.bottom, rect.left + cornerLen, rect.bottom, cornerPaint)
                canvas.drawLine(rect.left, rect.bottom, rect.left, rect.bottom - cornerLen, cornerPaint)
                // Bottom-right
                canvas.drawLine(rect.right, rect.bottom, rect.right - cornerLen, rect.bottom, cornerPaint)
                canvas.drawLine(rect.right, rect.bottom, rect.right, rect.bottom - cornerLen, cornerPaint)

                // Floating tooltip above/below the rectangle
                val hintText = "Solte para analisar com a IA"
                val textWidth = textPaint.measureText(hintText)
                val badgePadX = dpToPx(14f)
                val badgePadY = dpToPx(7f)

                val badgeY = if (rect.top > dpToPx(60f)) {
                    rect.top - dpToPx(24f)
                } else {
                    rect.bottom + dpToPx(34f)
                }

                val badgeRect = RectF(
                    rect.centerX() - textWidth / 2f - badgePadX,
                    badgeY - textPaint.textSize,
                    rect.centerX() + textWidth / 2f + badgePadX,
                    badgeY + badgePadY
                )

                canvas.drawRoundRect(badgeRect, dpToPx(16f), dpToPx(16f), badgeBackgroundPaint)
                canvas.drawRoundRect(badgeRect, dpToPx(16f), dpToPx(16f), badgeBorderPaint)
                canvas.drawText(hintText, rect.centerX(), badgeY, textPaint)
            }
        } else {
            // Idle top banner hint
            val idleText = "✂️ Arraste sobre a pergunta para recortar"
            val textWidth = textPaint.measureText(idleText)
            val badgePadX = dpToPx(18f)
            val badgePadY = dpToPx(10f)
            val bannerY = dpToPx(65f)

            val badgeRect = RectF(
                viewW / 2f - textWidth / 2f - badgePadX,
                bannerY - textPaint.textSize - badgePadY / 2f,
                viewW / 2f + textWidth / 2f + badgePadX,
                bannerY + badgePadY
            )

            canvas.drawRoundRect(badgeRect, dpToPx(20f), dpToPx(20f), badgeBackgroundPaint)
            canvas.drawRoundRect(badgeRect, dpToPx(20f), dpToPx(20f), badgeBorderPaint)
            canvas.drawText(idleText, viewW / 2f, bannerY, textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                startX = event.x
                startY = event.y
                currentX = event.x
                currentY = event.y
                isDragging = true
                invalidate()
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                currentX = event.x
                currentY = event.y
                invalidate()
                return true
            }
            MotionEvent.ACTION_UP -> {
                isDragging = false
                val rect = getSelectionRect()
                // Require minimum crop dimension (40px)
                if (rect.width() > dpToPx(30f) && rect.height() > dpToPx(30f)) {
                    onCropSelected(rect)
                } else {
                    // Small tap: reset selection without closing to allow another drag or button click
                    invalidate()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                invalidate()
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    private fun getSelectionRect(): RectF {
        val left = minOf(startX, currentX)
        val right = maxOf(startX, currentX)
        val top = minOf(startY, currentY)
        val bottom = maxOf(startY, currentY)
        return RectF(left, top, right, bottom)
    }
}
