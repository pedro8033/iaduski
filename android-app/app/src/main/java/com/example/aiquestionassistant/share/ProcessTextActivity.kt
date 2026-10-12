package com.example.aiquestionassistant.share

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import com.example.aiquestionassistant.service.FloatingBubbleService

class ProcessTextActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Ative a permissão de sobreposição no Assistente IA primeiro", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val selectedText = intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString()
            if (!selectedText.isNullOrBlank()) {
                if (!FloatingBubbleService.isRunning) {
                    val serviceIntent = Intent(this, FloatingBubbleService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent)
                    } else {
                        startService(serviceIntent)
                    }
                }

                FloatingBubbleService.instance?.let { service ->
                    service.setSharedContent(selectedText, null)
                    Toast.makeText(this, "Texto enviado para o Assistente IA", Toast.LENGTH_SHORT).show()
                } ?: run {
                    val updateIntent = Intent(this, FloatingBubbleService::class.java).apply {
                        putExtra("shared_text", selectedText)
                    }
                    startService(updateIntent)
                }
            }
        }

        finish()
    }
}
