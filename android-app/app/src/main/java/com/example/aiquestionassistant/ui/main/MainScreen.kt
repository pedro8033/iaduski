package com.example.aiquestionassistant.ui.main

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmarks
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Send
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.os.Bundle
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.navigation3.runtime.NavKey
import com.example.aiquestionassistant.NotesScreenKey
import com.example.aiquestionassistant.SettingsScreenKey
import com.example.aiquestionassistant.data.AiRepository
import com.example.aiquestionassistant.data.AppSettings
import com.example.aiquestionassistant.data.AssistantActionOptions
import com.example.aiquestionassistant.data.AssistantActionType
import com.example.aiquestionassistant.data.AssistantResult
import com.example.aiquestionassistant.data.HistoryRepository
import com.example.aiquestionassistant.service.FloatingBubbleService
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onItemClick: (NavKey) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var hasOverlayPermission by remember { mutableStateOf(checkOverlayPermission(context)) }
    var isServiceRunning by remember { mutableStateOf(FloatingBubbleService.isRunning) }
    var settings by remember { mutableStateOf(AppSettings.load(context)) }
    val historyRepo = remember { HistoryRepository(context) }
    val aiRepo = remember { AiRepository() }
    val clipboardManager = LocalClipboardManager.current

    // Estado da Interface Gemini e Janela Lateral (Drawer)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    var historyItems by remember { mutableStateOf<List<AssistantResult>>(emptyList()) }
    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen) {
            historyItems = historyRepo.getItems()
        }
    }

    var inputText by remember { mutableStateOf("") }
    var showToolsSheet by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var currentResult by remember { mutableStateOf<AssistantResult?>(null) }

    // TTS
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    var isTtsSpeaking by remember { mutableStateOf(false) }

    // Reconhecimento de Voz & Modo Conversa Live
    var speechRecognizer by remember { mutableStateOf<SpeechRecognizer?>(null) }
    var isListeningSpeech by remember { mutableStateOf(false) }
    var isLiveConversationMode by remember { mutableStateOf(false) }
    var baseTextBeforeListening by remember { mutableStateOf("") }
    var pendingFromLiveConversation by remember { mutableStateOf(false) }

    val mainHandler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
    var startListeningRef: ((Boolean) -> Unit)? by remember { mutableStateOf(null) }
    var executeActionRef: ((AssistantActionType, String?, Boolean) -> Unit)? by remember { mutableStateOf(null) }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startListeningRef?.invoke(pendingFromLiveConversation)
        } else {
            isLiveConversationMode = false
            Toast.makeText(context, "Permissão de microfone necessária para reconhecimento de voz", Toast.LENGTH_LONG).show()
        }
    }

    DisposableEffect(context) {
        val ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("pt", "BR")
            }
        }
        ttsInstance.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isTtsSpeaking = true
            }
            override fun onDone(utteranceId: String?) {
                isTtsSpeaking = false
                if (utteranceId == "tts_live_turn") {
                    mainHandler.postDelayed({
                        if (isLiveConversationMode) {
                            startListeningRef?.invoke(true)
                        }
                    }, 400)
                }
            }
            override fun onError(utteranceId: String?) {
                isTtsSpeaking = false
                if (utteranceId == "tts_live_turn") {
                    mainHandler.postDelayed({
                        if (isLiveConversationMode) {
                            startListeningRef?.invoke(true)
                        }
                    }, 400)
                }
            }
        })
        tts = ttsInstance
        onDispose {
            ttsInstance.stop()
            ttsInstance.shutdown()
            try {
                speechRecognizer?.destroy()
            } catch (_: Exception) {}
        }
    }

    val speakText: (String, Boolean) -> Unit = { textToSpeak, isLive ->
        val clean = textToSpeak.replace(Regex("""[#*`_]"""), "").trim()
        if (clean.isNotBlank()) {
            val utteranceId = if (isLive) "tts_live_turn" else "tts_main"
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            }
            tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            isTtsSpeaking = true
        } else if (isLive && isLiveConversationMode) {
            mainHandler.postDelayed({
                if (isLiveConversationMode) {
                    startListeningRef?.invoke(true)
                }
            }, 500)
        }
    }

    val stopTtsAudio: () -> Unit = {
        try {
            tts?.stop()
        } catch (_: Exception) {}
        isTtsSpeaking = false
    }

    val startListening: (Boolean) -> Unit = { fromLiveConversation ->
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            pendingFromLiveConversation = fromLiveConversation
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        } else if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Toast.makeText(context, "Reconhecimento de voz indisponível neste aparelho. Verifique o app Google.", Toast.LENGTH_LONG).show()
            if (fromLiveConversation) isLiveConversationMode = false
        } else {
            try {
                // Se já estiver falando pelo TTS, interrompe antes de ouvir
                stopTtsAudio()

                baseTextBeforeListening = if (fromLiveConversation) "" else inputText.trim()

                try {
                    speechRecognizer?.destroy()
                } catch (_: Exception) {}

                val recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                    setRecognitionListener(object : RecognitionListener {
                        override fun onReadyForSpeech(params: Bundle?) {
                            isListeningSpeech = true
                            Toast.makeText(context, if (fromLiveConversation) "IA ouvindo... fale agora" else "Pode falar agora...", Toast.LENGTH_SHORT).show()
                        }
                        override fun onBeginningOfSpeech() {
                            isListeningSpeech = true
                        }
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
                                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Tempo esgotado sem fala detectada"
                                else -> "Erro no reconhecimento de voz ($error)"
                            }
                            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()

                            // Se em conversa ao vivo o erro foi apenas timeout sem som, mantém o modo pronto
                            if (fromLiveConversation) {
                                if (error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT || error == SpeechRecognizer.ERROR_NO_MATCH) {
                                    // Continua em modo conversa aguardando nova tentativa ou prompt
                                } else {
                                    isLiveConversationMode = false
                                }
                            }
                        }
                        override fun onResults(results: Bundle?) {
                            isListeningSpeech = false
                            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            if (!matches.isNullOrEmpty()) {
                                val spoken = matches[0].trim()
                                val combined = if (baseTextBeforeListening.isNotBlank()) {
                                    "$baseTextBeforeListening $spoken"
                                } else {
                                    spoken
                                }
                                inputText = combined
                                if (fromLiveConversation) {
                                    Toast.makeText(context, "Pergunta enviada ao Gemini", Toast.LENGTH_SHORT).show()
                                    executeActionRef?.invoke(AssistantActionType.ASK, combined, true)
                                } else {
                                    Toast.makeText(context, "Voz reconhecida com sucesso!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        override fun onPartialResults(partialResults: Bundle?) {
                            val partialMatches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                            val partial = partialMatches?.firstOrNull()?.trim()
                            if (!partial.isNullOrBlank()) {
                                val combined = if (baseTextBeforeListening.isNotBlank()) {
                                    "$baseTextBeforeListening $partial"
                                } else {
                                    partial
                                }
                                inputText = combined
                            }
                        }
                        override fun onEvent(eventType: Int, params: Bundle?) {}
                    })
                }
                speechRecognizer = recognizer

                val lang = Locale.getDefault().toLanguageTag().ifBlank { "pt-BR" }
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, lang)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }
                recognizer.startListening(intent)
                isListeningSpeech = true
            } catch (e: Exception) {
                isListeningSpeech = false
                if (fromLiveConversation) isLiveConversationMode = false
                Toast.makeText(context, "Erro no microfone: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    startListeningRef = startListening

    val executeAction: (AssistantActionType, String?, Boolean) -> Unit = { action, customPrompt, autoSpeak ->
        val query = customPrompt ?: inputText
        if (query.isBlank()) {
            Toast.makeText(context, "Digite ou dite uma pergunta para analisar", Toast.LENGTH_SHORT).show()
        } else {
            isLoading = true
            inputText = ""
            scope.launch {
                val res = aiRepo.executeAction(
                    settings = settings,
                    action = action,
                    content = query,
                    prompt = null,
                    options = AssistantActionOptions(targetLanguage = settings.preferredLanguage)
                )
                isLoading = false
                currentResult = res
                if (res.isSuccess) {
                    historyRepo.saveItem(res)
                    if (autoSpeak) {
                        speakText(res.resultText, true)
                    }
                } else {
                    Toast.makeText(context, res.error ?: "Falha ao processar", Toast.LENGTH_LONG).show()
                    if (autoSpeak && isLiveConversationMode) {
                        speakText(res.error ?: "Não foi possível obter resposta agora. Pode tentar novamente.", true)
                    }
                }
            }
        }
    }

    executeActionRef = executeAction

    // Recheck permissions ao retomar
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission = checkOverlayPermission(context)
                isServiceRunning = FloatingBubbleService.isRunning
                settings = AppSettings.load(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = Color(0xFF11141C),
                drawerContentColor = Color.White,
                modifier = Modifier.width(310.dp)
            ) {
                // Header do Drawer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        GeminiSparkleStar(sizeDp = 26.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Gemini AI",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = { scope.launch { drawerState.close() } },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Fechar menu",
                            tint = Color.LightGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Botão Nova Conversa
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .clickable {
                            scope.launch { drawerState.close() }
                            inputText = ""
                            currentResult = null
                        },
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2330)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A3142))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Nova Conversa",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Cabeçalho da Seção de Histórico
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 16.dp, top = 8.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.History,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Histórico de Conversas",
                            color = Color(0xFF94A3B8),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (historyItems.isNotEmpty()) {
                        Text(
                            text = "Limpar",
                            color = Color(0xFFF87171),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable {
                                historyRepo.clearAll()
                                historyItems = emptyList()
                            }
                        )
                    }
                }

                // Lista de Histórico Rolável
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp)
                ) {
                    if (historyItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.Chat,
                                    contentDescription = null,
                                    tint = Color(0xFF2B2F3D),
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Nenhuma conversa gravada",
                                    color = Color.Gray,
                                    fontSize = 12.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(historyItems, key = { it.id }) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF171A23))
                                        .clickable {
                                            currentResult = item
                                            scope.launch { drawerState.close() }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when (item.action) {
                                            AssistantActionType.SUMMARIZE -> Icons.Default.Description
                                            AssistantActionType.DIAGRAM -> Icons.Default.AccountTree
                                            AssistantActionType.TRANSLATE -> Icons.Default.Translate
                                            AssistantActionType.IMAGE -> Icons.Default.Image
                                            AssistantActionType.EXPLAIN -> Icons.Default.Lightbulb
                                            AssistantActionType.FLASHCARDS -> Icons.Default.Style
                                            AssistantActionType.SOLVE -> Icons.Default.CheckCircle
                                            else -> Icons.Default.Chat
                                        },
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.title.ifBlank { item.action.label },
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = item.resultText.take(45),
                                            color = Color(0xFF94A3B8),
                                            fontSize = 10.sp,
                                            maxLines = 1
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            historyRepo.deleteItem(item.id)
                                            historyItems = historyRepo.getItems()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Excluir",
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF222634))

                // Seção Acesso Rápido
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Notas Inteligentes
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF171A23))
                            .clickable {
                                scope.launch { drawerState.close() }
                                onItemClick(NotesScreenKey)
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Bookmarks,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notas Inteligentes",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Anotações e cadernos com IA",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp
                            )
                        }
                    }

                    // Botão Flutuante (Bubble)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF171A23))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFA78BFA),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Botão Flutuante",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (isServiceRunning) "Ativo sobre o ecrã" else "Desativado",
                                    color = if (isServiceRunning) Color(0xFF34D399) else Color.Gray,
                                    fontSize = 10.sp
                                )
                            }
                        }
                        Switch(
                            checked = isServiceRunning,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (!hasOverlayPermission) {
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        context.startActivity(intent)
                                    } else {
                                        try {
                                            val intent = Intent(context, FloatingBubbleService::class.java)
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                context.startForegroundService(intent)
                                            } else {
                                                context.startService(intent)
                                            }
                                            isServiceRunning = true
                                        } catch (e: Exception) {
                                            isServiceRunning = false
                                            Toast.makeText(context, "Erro ao iniciar serviço flutuante: ${e.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                } else {
                                    try {
                                        val intent = Intent(context, FloatingBubbleService::class.java)
                                        context.stopService(intent)
                                    } catch (_: Exception) {}
                                    isServiceRunning = false
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF38BDF8)
                            )
                        )
                    }

                    // Definições no Drawer
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF171A23))
                            .clickable {
                                scope.launch { drawerState.close() }
                                onItemClick(SettingsScreenKey)
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = null,
                            tint = Color.LightGray,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Definições",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Modelos de IA, servidor e opções",
                                color = Color(0xFF94A3B8),
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFF222634))

                // Rodapé com Perfil do Usuário
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch { drawerState.close() }
                            onItemClick(SettingsScreenKey)
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF6366F1), Color(0xFF38BDF8))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "C",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("Cláudio", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(settings.model.ifBlank { "gemini-2.5-flash" }, color = Color(0xFF38BDF8), fontSize = 10.sp)
                    }
                }
            }
        }
    ) {
        // Fundo Preto Puro com Gradiente Sutil (Design Gemini - Imagem 3)
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFF000000),
                            Color(0xFF000000),
                            Color(0xFF030712),
                            Color(0xFF0A1128)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 90.dp) // Espaço para a barra inferior fixa
            ) {
                // TOP BAR (☰ à esquerda, Gemini com Estrela no Centro, Avatar C à direita)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Menu Hamburger / Suspenso (Abre a janela lateral à esquerda)
                    IconButton(onClick = { scope.launch { drawerState.open() } }) {
                        Icon(
                            Icons.Default.Menu,
                            contentDescription = "Menu Principal",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Título Central: Gemini com Estrela Sparkle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        GeminiSparkleStar(sizeDp = 22.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Gemini",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    // Avatar do Usuário (Foto C) -> Ao clicar vai direto para as Definições
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF6366F1), Color(0xFF38BDF8))
                                )
                            )
                            .clickable { onItemClick(SettingsScreenKey) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "C",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

            // CORPO CENTRAL: Estado Inicial com Estrela Gemini OU Exibição do Resultado
            if (currentResult == null && !isLoading) {
                // ESTADO INICIAL GEMINI (IMAGEM 3)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Estrela Multicolorida Gemini
                        GeminiSparkleStar(sizeDp = 52.dp)

                        Spacer(modifier = Modifier.height(28.dp))

                        // Saudação elegante
                        Text(
                            text = "O que se segue,\nCláudio?",
                            fontSize = 30.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            lineHeight = 38.sp
                        )
                    }
                }
            } else if (isLoading) {
                // Loading
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color(0xFF38BDF8))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "A consultar o Gemini...",
                            color = Color.LightGray,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                // RESULTADO ATIVO DO ASSISTENTE
                currentResult?.let { result ->
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(scrollState),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2026)),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = result.title.ifBlank { result.action.label },
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF38BDF8),
                                        fontSize = 17.sp
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        // Ouvir TTS
                                        IconButton(onClick = {
                                            if (isTtsSpeaking) {
                                                tts?.stop()
                                                isTtsSpeaking = false
                                            } else {
                                                tts?.speak(result.resultText, TextToSpeech.QUEUE_FLUSH, null, "tts_id")
                                                isTtsSpeaking = true
                                            }
                                        }) {
                                            Icon(
                                                if (isTtsSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                                                contentDescription = "Ouvir",
                                                tint = Color.LightGray
                                            )
                                        }
                                        // Copiar
                                        IconButton(onClick = {
                                            clipboardManager.setText(AnnotatedString(result.resultText))
                                            Toast.makeText(context, "Copiado!", Toast.LENGTH_SHORT).show()
                                        }) {
                                            Icon(
                                                Icons.Default.ContentCopy,
                                                contentDescription = "Copiar",
                                                tint = Color.LightGray
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Texto principal
                                Text(
                                    text = result.resultText,
                                    color = Color(0xFFE2E8F0),
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp
                                )

                                // Diagrama Mermaid
                                if (!result.diagramCode.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF121418)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text("📊 Diagrama Mermaid:", color = Color(0xFFA855F7), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = result.diagramCode,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                color = Color(0xFFA7F3D0)
                                            )
                                        }
                                    }
                                }

                                // Imagem IA Gerada
                                if (!result.imageUrl.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF13151B)),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Column {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFFEC4899), modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Imagem gerada por IA", color = Color(0xFFF472B6), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                }
                                            }

                                            // Exibição da imagem em rede com loader
                                            AsyncNetworkImage(
                                                url = result.imageUrl,
                                                modifier = Modifier.fillMaxWidth()
                                            )

                                            // Ações da Imagem
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(Color(0xFF1C1E26))
                                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                                horizontalArrangement = Arrangement.End,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Button(
                                                    onClick = {
                                                        clipboardManager.setText(AnnotatedString(result.imageUrl))
                                                        Toast.makeText(context, "Link da imagem copiado!", Toast.LENGTH_SHORT).show()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF272935)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.height(34.dp)
                                                ) {
                                                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Copiar Link", color = Color.White, fontSize = 12.sp)
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Button(
                                                    onClick = {
                                                        try {
                                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(result.imageUrl))
                                                            context.startActivity(intent)
                                                        } catch (e: Exception) {
                                                            Toast.makeText(context, "Erro ao abrir navegador", Toast.LENGTH_SHORT).show()
                                                        }
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDB2777)),
                                                    shape = RoundedCornerShape(8.dp),
                                                    modifier = Modifier.height(34.dp)
                                                ) {
                                                    Icon(Icons.Default.OpenInNew, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text("Abrir em HD", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // PÍLULA FLUTUANTE INFERIOR (DESIGN EXATO GEMINI - IMAGEM 3)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            // Banner de Status da Conversa Live Contínua por Voz
            if (isLiveConversationMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Color(0xFF0C1929))
                        .border(1.dp, Color(0xFF0284C7), RoundedCornerShape(18.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = null,
                            tint = if (isListeningSpeech) Color(0xFF38BDF8) else Color(0xFFF59E0B),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                isListeningSpeech -> "IA ouvindo... Fale a sua pergunta"
                                isLoading -> "Gemini a processar resposta..."
                                isTtsSpeaking -> "Gemini a responder por voz..."
                                else -> "Modo de conversa ativo (aguardando fala)"
                            },
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1
                        )
                    }
                    Text(
                        text = "Encerrar",
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                isLiveConversationMode = false
                                stopTtsAudio()
                                try {
                                    speechRecognizer?.stopListening()
                                } catch (_: Exception) {}
                                isListeningSpeech = false
                                Toast.makeText(context, "Conversa por voz encerrada", Toast.LENGTH_SHORT).show()
                            }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(36.dp))
                    .background(Color(0xFF1E2026))
                    .border(1.dp, Color(0xFF2C2F38), RoundedCornerShape(36.dp))
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Botão "+" (Abre BottomSheet com as 8 opções modernas sem emojis)
                IconButton(onClick = { showToolsSheet = true }) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Opções e Ferramentas",
                        tint = Color(0xFFE2E8F0),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Campo de Texto Transparente "Pedir ao Gemini..."
                BasicTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 16.sp
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (inputText.isNotBlank()) {
                                executeAction(AssistantActionType.ASK, inputText, isLiveConversationMode)
                            }
                        }
                    ),
                    decorationBox = { innerTextField ->
                        if (inputText.isEmpty()) {
                            Text(
                                text = if (isListeningSpeech) "Ouvindo voz em tempo real..." else "Pedir ao Gemini...",
                                color = if (isListeningSpeech) Color(0xFF38BDF8) else Color(0xFF8E95A5),
                                fontSize = 16.sp
                            )
                        }
                        innerTextField()
                    }
                )

                // Botão Microfone (Gravação / Transcrição com feedback visual e toggle de interrupção)
                IconButton(
                    onClick = {
                        if (isListeningSpeech) {
                            try {
                                speechRecognizer?.stopListening()
                            } catch (_: Exception) {}
                            isListeningSpeech = false
                            Toast.makeText(context, "Gravação de voz finalizada", Toast.LENGTH_SHORT).show()
                        } else {
                            startListening(false)
                        }
                    },
                    modifier = if (isListeningSpeech) Modifier.clip(CircleShape).background(Color(0xFF0F2D4A)) else Modifier
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = if (isListeningSpeech) "Parar gravação" else "Entrada de Voz",
                        tint = if (isListeningSpeech) Color(0xFF38BDF8) else Color(0xFFE2E8F0),
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Se o usuário digitou texto, exibe a SETA DE ENVIAR; senão, exibe o botão Live de Conversa por Voz
                if (inputText.isNotBlank()) {
                    IconButton(
                        onClick = {
                            executeAction(AssistantActionType.ASK, inputText, isLiveConversationMode)
                        },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1D51D8))
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Enviar Mensagem",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    // Botão Azul com Ondas Sonoras Live ||| (Conversa Contínua por Voz / TTS)
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(if (isLiveConversationMode || isListeningSpeech) Color(0xFF0284C7) else Color(0xFF1D51D8))
                            .clickable {
                                if (isLiveConversationMode) {
                                    isLiveConversationMode = false
                                    stopTtsAudio()
                                    try {
                                        speechRecognizer?.stopListening()
                                    } catch (_: Exception) {}
                                    isListeningSpeech = false
                                    Toast.makeText(context, "Conversa por voz encerrada", Toast.LENGTH_SHORT).show()
                                } else {
                                    isLiveConversationMode = true
                                    startListening(true)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.width(3.dp).height(if (isListeningSpeech || isLiveConversationMode) 16.dp else 12.dp).background(Color.White, RoundedCornerShape(2.dp)))
                            Box(modifier = Modifier.width(3.dp).height(if (isListeningSpeech || isLiveConversationMode) 22.dp else 18.dp).background(Color.White, RoundedCornerShape(2.dp)))
                        }
                    }
                }
            }
        }
    }

    // =========================================================================
    // MODAL BOTTOM SHEET: AS 8 FERRAMENTAS DO APP (IMAGEM 2 - ÍCONES MODERNOS SEM EMOJIS)
    // =========================================================================
    if (showToolsSheet) {
        ModalBottomSheet(
            onDismissRequest = { showToolsSheet = false },
            containerColor = Color(0xFF16181D),
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Ações do Assistente IA",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Selecione a inteligência que deseja aplicar",
                    fontSize = 13.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Grid 2 Colunas com as 8 Ferramentas (Ícones Modernos, Sem Emojis)
                val modernTools = listOf(
                    Triple(AssistantActionType.SUMMARIZE, "Sintetiza pontos-chave", Icons.Default.Description),
                    Triple(AssistantActionType.EXPLAIN, "Didática passo a passo", Icons.Default.Lightbulb),
                    Triple(AssistantActionType.DIAGRAM, "Gráfico visual Mermaid", Icons.Default.AccountTree),
                    Triple(AssistantActionType.IMAGE, "Arte conceitual de IA", Icons.Default.Image),
                    Triple(AssistantActionType.SOLVE, "Gabarito e análise", Icons.Default.CheckCircle),
                    Triple(AssistantActionType.FLASHCARDS, "Memorização ativa", Icons.Default.Style),
                    Triple(AssistantActionType.TRANSLATE, "Adaptação multilíngue", Icons.Default.Translate),
                    Triple(AssistantActionType.ASK, "Tire qualquer dúvida", Icons.Default.Chat)
                )

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    modernTools.chunked(2).forEach { rowTools ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            rowTools.forEach { (action, subtitle, iconVector) ->
                                ModernAppToolCard(
                                    title = action.label,
                                    subtitle = subtitle,
                                    icon = iconVector,
                                    modifier = Modifier.weight(1f),
                                    onClick = {
                                        showToolsSheet = false
                                        executeAction(action, null, false)
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    }
}

/**
 * Cartão de Ferramenta Moderna (Imagem 2 - com ícone Vetorial SVG/Material, sem emojis)
 */
@Composable
fun ModernAppToolCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2128)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2B2E38)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF272B35)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                Text(
                    text = subtitle,
                    color = Color(0xFF8E95A5),
                    fontSize = 11.sp,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Estrela Multicolorida Gemini (4 Pontas Côncavas com Gradiente Cyan, Indigo, Pink, Amber)
 */
@Composable
fun GeminiSparkleStar(
    modifier: Modifier = Modifier,
    sizeDp: Dp = 48.dp
) {
    Canvas(modifier = modifier.size(sizeDp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        val path = Path().apply {
            moveTo(cx, 0f)
            quadraticBezierTo(cx, cy, w, cy)
            quadraticBezierTo(cx, cy, cx, h)
            quadraticBezierTo(cx, cy, 0f, cy)
            quadraticBezierTo(cx, cy, cx, 0f)
            close()
        }

        val gradient = Brush.linearGradient(
            colors = listOf(
                Color(0xFF38BDF8), // Cyan
                Color(0xFF818CF8), // Indigo
                Color(0xFFF472B6), // Pink
                Color(0xFFFBBF24)  // Amber
            ),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        )

        drawPath(path, brush = gradient)
    }
}

fun checkOverlayPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        Settings.canDrawOverlays(context)
    } else {
        true
    }
}

@Composable
fun AsyncNetworkImage(
    url: String,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(url) { mutableStateOf<ImageBitmap?>(null) }
    var isLoading by remember(url) { mutableStateOf(true) }
    var hasError by remember(url) { mutableStateOf(false) }

    LaunchedEffect(url) {
        isLoading = true
        hasError = false
        withContext(Dispatchers.IO) {
            try {
                val req = Request.Builder().url(url).build()
                val okHttpClient = OkHttpClient.Builder()
                    .connectTimeout(25, TimeUnit.SECONDS)
                    .readTimeout(45, TimeUnit.SECONDS)
                    .build()
                okHttpClient.newCall(req).execute().use { resp ->
                    if (resp.isSuccessful) {
                        resp.body?.byteStream()?.use { stream ->
                            val decoded = BitmapFactory.decodeStream(stream)
                            if (decoded != null) {
                                bitmap = decoded.asImageBitmap()
                            } else {
                                hasError = true
                            }
                        } ?: run { hasError = true }
                    } else {
                        hasError = true
                    }
                }
            } catch (_: Exception) {
                hasError = true
            } finally {
                isLoading = false
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 180.dp, max = 360.dp)
            .background(Color(0xFF111216)),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(24.dp)
            ) {
                CircularProgressIndicator(color = Color(0xFFEC4899), modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.height(10.dp))
                Text("A carregar imagem de IA em alta resolução...", color = Color.LightGray, fontSize = 12.sp)
            }
        } else if (bitmap != null) {
            Image(
                bitmap = bitmap!!,
                contentDescription = "Imagem gerada por IA",
                modifier = Modifier.fillMaxWidth(),
                contentScale = ContentScale.Crop
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(Icons.Default.BrokenImage, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.height(6.dp))
                Text("Prévia direta indisponível. Toque em 'Abrir em HD' para ver.", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}
