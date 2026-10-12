package com.example.aiquestionassistant.share

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Base64
import android.widget.Toast
import com.example.aiquestionassistant.service.FloatingBubbleService
import java.io.ByteArrayOutputStream

class ShareReceiverActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Ative a permissão de sobreposição no Assistente IA primeiro", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        handleShareIntent(intent)
        finish()
    }

    private fun handleShareIntent(intent: Intent?) {
        if (intent == null || intent.action != Intent.ACTION_SEND) return

        var sharedText: String? = null
        var sharedImageBase64: String? = null

        val type = intent.type ?: ""
        if (type.startsWith("text/")) {
            sharedText = intent.getStringExtra(Intent.EXTRA_TEXT) ?: intent.getStringExtra(Intent.EXTRA_SUBJECT)
        } else if (type.startsWith("image/")) {
            val imageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
            } else {
                @Suppress("DEPRECATION")
                intent.getParcelableExtra(Intent.EXTRA_STREAM)
            }

            if (imageUri != null) {
                sharedImageBase64 = uriToBase64(imageUri)
            }
        }

        // Garante que o serviço do assistente flutuante está a correr
        if (!FloatingBubbleService.isRunning) {
            val serviceIntent = Intent(this, FloatingBubbleService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }

        // Envia o conteúdo partilhado para o assistente flutuante
        FloatingBubbleService.instance?.let { service ->
            service.setSharedContent(sharedText, sharedImageBase64)
            Toast.makeText(this, "Conteúdo carregado no Assistente IA", Toast.LENGTH_SHORT).show()
        } ?: run {
            // Se a instância ainda estiver a inicializar, envia através do intent do serviço
            val updateIntent = Intent(this, FloatingBubbleService::class.java).apply {
                putExtra("shared_text", sharedText)
                putExtra("shared_image", sharedImageBase64)
            }
            startService(updateIntent)
        }
    }

    private fun uriToBase64(uri: Uri): String? {
        return try {
            contentResolver.openInputStream(uri)?.use { stream ->
                val bitmap = BitmapFactory.decodeStream(stream)
                val out = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
