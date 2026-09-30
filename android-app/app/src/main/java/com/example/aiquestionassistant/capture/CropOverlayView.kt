package com.example.aiquestionassistant.capture

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View

class CropOverlayView(
    context: Context,
    private val onCropSelected: (RectF) -> Unit,
    private val onCancel: () -> Unit
) : View(context) {

    private val backgroundPaint = Paint().apply {
        color = Color.parseColor("#B3000000") // 70% black dim
    }

    private val clearPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
    }

    private val borderPaint = Paint().apply {
        color = Color.parseColor("#38BDF8") // Cyan accent
        style = Paint.Style.STROKE
        strokeWidth = 5f
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 42f
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    private var startX = 0f
    private var startY = 0f
    private var currentX = 0f
    private var currentY = 0f
    private var isDragging = false

    init {
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw dimmed background
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), backgroundPaint)

        if (isDragging) {
            val rect = getSelectionRect()
            // Clear selection inside rectangle
            canvas.drawRect(rect, clearPaint)
            // Draw border
            canvas.drawRect(rect, borderPaint)

            // Draw hint
            val hintY = if (rect.top > 80f) rect.top - 20f else rect.bottom + 60f
            canvas.drawText("Solte para analisar com a IA", rect.centerX(), hintY, textPaint)
        } else {
            canvas.drawText("Arraste o dedo para recortar a pergunta ou símbolos", width / 2f, height / 2f, textPaint)
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
                // Require minimum drag size (e.g. 50x50 px)
                if (rect.width() > 50 && rect.height() > 50) {
                    onCropSelected(rect)
                } else {
                    onCancel()
                }
                return true
            }
            MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                onCancel()
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
