package com.example.aiquestionassistant.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class AiRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun solve(
        settings: AppSettings,
        questionText: String? = null,
        imageBase64: String? = null
    ): SolveResult = withContext(Dispatchers.IO) {
        try {
            // Se tiver Chave direta do Gemini configurada, usa diretamente (autónomo no telemóvel)
            if (settings.directGeminiKey.isNotBlank()) {
                return@withContext solveDirectGemini(settings, questionText, imageBase64)
            } else {
                // Caso contrário, conecta ao servidor backend
                return@withContext solveViaBackend(settings, questionText, imageBase64)
            }
        } catch (e: Exception) {
            return@withContext SolveResult(
                isSuccess = false,
                error = "Erro ao comunicar com a IA: ${e.localizedMessage ?: e.message}"
            )
        }
    }

    private fun solveViaBackend(
        settings: AppSettings,
        questionText: String?,
        imageBase64: String?
    ): SolveResult {
        val baseUrl = settings.serverUrl.trimEnd('/')
        val endpoint = "$baseUrl/api/solve"

        val jsonBody = JSONObject().apply {
            put("questionText", questionText ?: "")
            if (!imageBase64.isNullOrBlank()) {
                val formatted = if (imageBase64.startsWith("data:")) imageBase64 else "data:image/jpeg;base64,$imageBase64"
                put("screenshot", formatted)
            }
            put("model", settings.model)
            put("mode", settings.mode)
        }

        val requestBody = jsonBody.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(endpoint)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                return SolveResult(
                    isSuccess = false,
                    error = "Servidor respondeu com erro ${response.code}: $responseBody"
                )
            }

            val json = JSONObject(responseBody)
            val success = json.optBoolean("success", true)
            if (!success) {
                return SolveResult(
                    isSuccess = false,
                    error = json.optString("error", "Erro desconhecido retornado pelo servidor")
                )
            }

            val answer = json.optString("answer", "")
            val correctOption = json.optString("correctOption", "").ifBlank {
                extractOption(answer)
            }
            val explanation = json.optString("explanation", "").ifBlank { answer }

            return SolveResult(
                isSuccess = true,
                correctOption = correctOption,
                explanation = explanation,
                rawText = answer
            )
        }
    }

    private fun solveDirectGemini(
        settings: AppSettings,
        questionText: String?,
        imageBase64: String?
    ): SolveResult {
        val model = settings.model.ifBlank { "gemini-2.5-flash" }
        val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent?key=${settings.directGeminiKey.trim()}"

        val systemPrompt = """
            És um assistente de elite especializado em resolver questões de provas, concursos e testes psicotécnicos com imagens, símbolos, rotações de figuras e sequências lógicas.
            Analisa a imagem e o texto com máxima precisão.
            Estrutura a tua resposta obrigatoriamente assim:
            **OPÇÃO CORRETA:** [Indica a letra/número da alternativa certa e breve descrição]
            **EXPLICAÇÃO:** [Explica detalhadamente o passo a passo da lógica, da rotação ou do padrão de símbolos].
        """.trimIndent()

        val partsArray = JSONArray()

        val fullPrompt = buildString {
            append(systemPrompt)
            if (!questionText.isNullOrBlank()) {
                append("\n\nPergunta adicional do utilizador:\n").append(questionText)
            }
        }
        partsArray.put(JSONObject().put("text", fullPrompt))

        if (!imageBase64.isNullOrBlank()) {
            val cleanBase64 = if (imageBase64.contains(",")) {
                imageBase64.substringAfter(",")
            } else {
                imageBase64
            }
            val inlineData = JSONObject().apply {
                put("mimeType", "image/jpeg")
                put("data", cleanBase64)
            }
            partsArray.put(JSONObject().put("inlineData", inlineData))
        }

        val contentsArray = JSONArray().put(JSONObject().put("parts", partsArray))
        val requestJson = JSONObject().apply {
            put("contents", contentsArray)
        }

        val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(endpoint)
            .post(requestBody)
            .build()

        client.newCall(request).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                return SolveResult(
                    isSuccess = false,
                    error = "Erro da API Gemini (${response.code}): $responseBody"
                )
            }

            val json = JSONObject(responseBody)
            val candidates = json.optJSONArray("candidates")
            if (candidates == null || candidates.length() == 0) {
                return SolveResult(isSuccess = false, error = "A IA não retornou nenhuma resposta")
            }

            val candidate = candidates.getJSONObject(0)
            val parts = candidate.optJSONObject("content")?.optJSONArray("parts")
            val textBuilder = StringBuilder()
            if (parts != null) {
                for (i in 0 until parts.length()) {
                    textBuilder.append(parts.getJSONObject(i).optString("text", ""))
                }
            }
            val answer = textBuilder.toString()
            val correctOption = extractOption(answer)

            return SolveResult(
                isSuccess = true,
                correctOption = correctOption,
                explanation = answer,
                rawText = answer
            )
        }
    }

    private fun extractOption(text: String): String {
        val lines = text.lines()
        for (line in lines) {
            val lower = line.lowercase()
            if (lower.contains("opção correta:") || lower.contains("alternativa correta:") || lower.contains("resposta correta:")) {
                return line.substringAfter(":").trim().removePrefix("**").removeSuffix("**").trim()
            }
        }
        val match = Regex("""(?:Opção|Alternativa|Resposta)\s*([A-E1-9])""", RegexOption.IGNORE_CASE).find(text)
        return match?.value ?: "Ver explicação"
    }
}
