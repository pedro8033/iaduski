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
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Style
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Layers
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.style.TextOverflow
import com.example.aiquestionassistant.NoteEditorKey
import com.example.aiquestionassistant.data.NotesLocalRepository
import com.example.aiquestionassistant.data.Note
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Send
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
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
    val notesRepo = remember { NotesLocalRepository(context) }
    var notesItems by remember { mutableStateOf<List<Note>>(emptyList()) }
    var historySearchQuery by remember { mutableStateOf("") }

    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen) {
            historyItems = historyRepo.getItems()
            notesItems = try { notesRepo.getNotes() } catch (_: Exception) { emptyList() }
        }
    }

    var inputText by remember { mutableStateOf("") }
    var showToolsSheet by remember { mutableStateOf(false) }

    var isLoading by remember { mutableStateOf(false) }
    var currentResult by remember { mutableStateOf<AssistantResult?>(null) }

    // Mensagens da conversa (inicialmente idênticas à imagem de referência)
    var chatMessages by remember {
        mutableStateOf(
            listOf(
                ChatMessage(
                    isUser = true,
                    text = "Oi"
                ),
                ChatMessage(
                    isUser = false,
                    text = "Oiii! \uD83D\uDE04\uD83D\uDC4B Tudo bem contigo?\n\nComo está a correr o teu dia? Diz-me, em que posso ajudar hoje? \uD83D\uDE80"
                ),
                ChatMessage(
                    isUser = true,
                    text = "Em que ano foi fundada angola?"
                ),
                ChatMessage(
                    isUser = false,
                    text = "Angola tornou-se independente em **1975**, no dia **11 de novembro**. \uD83C\uDDE6\uD83C\uDDF4\n\nNo entanto, é importante distinguir duas datas:\n\n• **1575**: Fundação de Luanda, por Paulo Dias de Novais, durante a presença colonial portuguesa.\n• **11 de novembro de 1975**: Proclamação da Independência de Angola, sob a liderança de Agostinho Neto, o primeiro Presidente da República."
                )
            )
        )
    }

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
            val userMsg = ChatMessage(isUser = true, text = query)
            chatMessages = chatMessages + userMsg
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
                    chatMessages = chatMessages + ChatMessage(isUser = false, text = res.resultText, result = res)
                    historyRepo.saveItem(res)
                    if (autoSpeak) {
                        speakText(res.resultText, true)
                    }
                } else {
                    val err = res.error ?: "Falha ao processar resposta. Tente novamente."
                    chatMessages = chatMessages + ChatMessage(isUser = false, text = err, result = res)
                    Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                    if (autoSpeak && isLiveConversationMode) {
                        speakText(err, true)
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
                drawerContainerColor = Color(0xFF000000),
                drawerContentColor = Color.White,
                modifier = Modifier
                    .fillMaxHeight()
                    .width(320.dp)
            ) {
                // Header do Drawer: "Gemini" à esquerda, "✕" à direita
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 22.dp, end = 16.dp, top = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gemini",
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Normal
                    )
                    IconButton(
                        onClick = { scope.launch { drawerState.close() } },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Fechar menu",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Opções Principais da Sidebar
                // 1. Novo chat
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            chatMessages = emptyList()
                            inputText = ""
                            currentResult = null
                            scope.launch { drawerState.close() }
                            Toast.makeText(context, "Novo chat iniciado", Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 22.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Novo chat",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(18.dp))
                    Text(
                        text = "Novo chat",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                // 2. Pesquisar chats (Campo de busca interativo com filtro em tempo real)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 22.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Search,
                        contentDescription = "Pesquisar chats",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(18.dp))
                    Box(modifier = Modifier.weight(1f)) {
                        if (historySearchQuery.isEmpty()) {
                            Text(
                                text = "Pesquisar chats",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                        BasicTextField(
                            value = historySearchQuery,
                            onValueChange = { historySearchQuery = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (historySearchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { historySearchQuery = "" },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Limpar pesquisa",
                                tint = Color(0xFFA0A0A0),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // 3. Estudantes
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch { drawerState.close() }
                            onItemClick(NotesScreenKey)
                        }
                        .padding(horizontal = 22.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.School,
                        contentDescription = "Estudantes",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(18.dp))
                    Text(
                        text = "Estudantes",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                // 7. Biblioteca
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch { drawerState.close() }
                            onItemClick(NotesScreenKey)
                        }
                        .padding(horizontal = 22.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.GridView,
                        contentDescription = "Biblioteca",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(18.dp))
                    Text(
                        text = "Biblioteca",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Normal
                    )
                }

                // 8. Botão Flutuante (Preservando switch, sobreposição e serviço)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 22.dp, end = 16.dp, top = 6.dp, bottom = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Layers,
                            contentDescription = "Botão Flutuante",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(18.dp))
                        Column {
                            Text(
                                text = "Botão Flutuante",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal
                            )
                            Text(
                                text = if (isServiceRunning) "Ativo sobre o ecrã" else "Desativado",
                                color = if (isServiceRunning) Color(0xFF38BDF8) else Color(0xFF71717A),
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
                            checkedTrackColor = Color(0xFF38BDF8),
                            uncheckedThumbColor = Color(0xFF9E9E9E),
                            uncheckedTrackColor = Color(0xFF262626)
                        )
                    )
                }

                // Seção Notebooks
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 22.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Notebooks",
                        color = Color(0xFF9E9E9E),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    if (historyItems.isNotEmpty()) {
                        Text(
                            text = "Limpar",
                            color = Color(0xFFEF4444),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.clickable {
                                historyRepo.clearAll()
                                historyItems = emptyList()
                            }
                        )
                    }
                }


                // Lista de Itens Rolável
                val filteredHistory = remember(historyItems, historySearchQuery) {
                    if (historySearchQuery.isBlank()) historyItems
                    else historyItems.filter {
                        it.title.contains(historySearchQuery, ignoreCase = true) ||
                        it.resultText.contains(historySearchQuery, ignoreCase = true)
                    }
                }
                val filteredNotes = remember(notesItems, historySearchQuery) {
                    if (historySearchQuery.isBlank()) notesItems
                    else notesItems.filter {
                        it.title.contains(historySearchQuery, ignoreCase = true) ||
                        it.content.contains(historySearchQuery, ignoreCase = true)
                    }
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    if (filteredHistory.isEmpty() && filteredNotes.isEmpty()) {
                        Text(
                            text = if (historySearchQuery.isNotBlank()) "Nenhum resultado" else "Nenhum chat ou notebook recente",
                            color = Color(0xFF71717A),
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredNotes, key = { "note_${it.id}" }) { note ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            scope.launch { drawerState.close() }
                                            onItemClick(NoteEditorKey(note.id))
                                        }
                                        .padding(horizontal = 22.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(18.dp))
                                    Text(
                                        text = note.title.ifBlank { "Untitled notebook" },
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            items(filteredHistory, key = { "history_${it.id}" }) { item ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            currentResult = item
                                            chatMessages = listOf(
                                                ChatMessage(isUser = true, text = item.title.ifBlank { item.action.label }),
                                                ChatMessage(isUser = false, text = item.resultText, result = item)
                                            )
                                            scope.launch { drawerState.close() }
                                        }
                                        .padding(horizontal = 22.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.MenuBook,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(18.dp))
                                    Text(
                                        text = item.title.ifBlank { item.action.label },
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Normal,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            historyRepo.deleteItem(item.id)
                                            historyItems = historyRepo.getItems()
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Excluir",
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            item {
                                Text(
                                    text = "Todos os notebooks",
                                    color = Color(0xFF71717A),
                                    fontSize = 13.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            scope.launch { drawerState.close() }
                                            onItemClick(NotesScreenKey)
                                        }
                                        .padding(horizontal = 22.dp, vertical = 14.dp)
                                )
                            }
                        }
                    }
                }

                // Rodapé com Perfil do Usuário (Fiel à imagem de referência Gemini)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            scope.launch { drawerState.close() }
                            onItemClick(SettingsScreenKey)
                        }
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .border(1.5.dp, Color(0xFF38BDF8), CircleShape)
                            .background(
                                Brush.radialGradient(
                                    listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "C",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Cláudio Duski",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "PRO",
                            color = Color(0xFF9E9E9E),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Normal,
                            letterSpacing = 0.5.sp
                        )
                    }
                    IconButton(
                        onClick = {
                            scope.launch { drawerState.close() }
                            onItemClick(SettingsScreenKey)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Definições",
                            tint = Color(0xFF9E9E9E),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    ) {
        // Fundo Preto Puro (Fiel à imagem de referência ChatGPT)
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFF000000))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                // TOP BAR (Fiel à imagem de referência: Botão "=" à esquerda, Cápsula [Novo Chat 📝 | Mais Opções ⋮] à direita)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botão circular com "=" à esquerda (abre menu lateral)
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF212121))
                            .clickable { scope.launch { drawerState.open() } },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(modifier = Modifier.width(16.dp).height(2.5.dp).background(Color.White, RoundedCornerShape(1.5.dp)))
                            Box(modifier = Modifier.width(16.dp).height(2.5.dp).background(Color.White, RoundedCornerShape(1.5.dp)))
                        }
                    }

                    // Cápsula superior direita [ Novo Chat 📝 | Mais Opções ⋮ ]
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(22.dp))
                            .background(Color(0xFF212121))
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                chatMessages = emptyList()
                                inputText = ""
                                currentResult = null
                                Toast.makeText(context, "Nova conversa iniciada", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Nova Conversa",
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }

                        var showTopMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(
                                onClick = { showTopMenu = true },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Default.MoreVert,
                                    contentDescription = "Mais opções",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            DropdownMenu(
                                expanded = showTopMenu,
                                onDismissRequest = { showTopMenu = false },
                                containerColor = Color(0xFF1E2128)
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Nova Conversa", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, tint = Color(0xFF38BDF8)) },
                                    onClick = {
                                        showTopMenu = false
                                        chatMessages = emptyList()
                                        inputText = ""
                                        currentResult = null
                                        Toast.makeText(context, "Nova conversa iniciada", Toast.LENGTH_SHORT).show()
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Notas Inteligentes", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.Bookmarks, contentDescription = null, tint = Color(0xFF38BDF8)) },
                                    onClick = {
                                        showTopMenu = false
                                        onItemClick(NotesScreenKey)
                                    }
                                )
                                DropdownMenuItem(
                                    text = { Text("Definições", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.Settings, contentDescription = null, tint = Color(0xFF38BDF8)) },
                                    onClick = {
                                        showTopMenu = false
                                        onItemClick(SettingsScreenKey)
                                    }
                                )
                                HorizontalDivider(color = Color(0xFF2C2F38))
                                DropdownMenuItem(
                                    text = { Text("Limpar Mensagens", color = Color(0xFFF87171)) },
                                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFF87171)) },
                                    onClick = {
                                        showTopMenu = false
                                        chatMessages = emptyList()
                                        inputText = ""
                                        currentResult = null
                                        Toast.makeText(context, "Conversa limpa", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }

                // CORPO CENTRAL: FEED DA CONVERSA (Fiel à imagem de referência)
                val listState = rememberLazyListState()

                LaunchedEffect(chatMessages.size, isLoading) {
                    val totalCount = chatMessages.size + if (isLoading) 1 else 0
                    if (totalCount > 0) {
                        listState.animateScrollToItem(totalCount - 1)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 84.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(chatMessages, key = { it.id }) { msg ->
                        if (msg.isUser) {
                            UserMessageItem(text = msg.text)
                        } else {
                            AssistantMessageItem(
                                message = msg,
                                isTtsSpeaking = isTtsSpeaking,
                                onSpeak = { speakText(msg.text, false) },
                                onStopTts = stopTtsAudio,
                                onOpenNotes = { onItemClick(NotesScreenKey) }
                            )
                        }
                    }

                    // Indicador animado durante a resposta da IA (Apenas os 3 pontos ● ● ●, SEM texto)
                    if (isLoading) {
                        item(key = "loading_dots_chat") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 6.dp, bottom = 10.dp),
                                horizontalArrangement = Arrangement.Start
                            ) {
                                TypingBouncingDots(
                                    dotSize = 6.5.dp,
                                    dotColor = Color(0xFFE5E7EB),
                                    spacing = 6.dp,
                                    travelDistance = 5.5.dp
                                )
                            }
                        }
                    }
                }
            }

            // BARRA INFERIOR (Cápsula flutuante fiel à imagem de referência: [+] [Responder ao ChatGPT] [🎤] [🔵||||])
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                // Banner de Status da Conversa Live Contínua por Voz (quando ativo)
                if (isLiveConversationMode) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
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
                            if (isLoading) {
                                // Sem nenhum texto! Apenas os 3 pontos animados
                                TypingBouncingDots(
                                    dotSize = 5.dp,
                                    dotColor = Color(0xFF38BDF8),
                                    spacing = 4.dp,
                                    travelDistance = 3.5.dp
                                )
                            } else {
                                Text(
                                    text = when {
                                        isListeningSpeech -> "IA ouvindo... Fale a sua pergunta"
                                        isTtsSpeaking -> "IA a responder por voz..."
                                        else -> "Modo conversa ativo (aguardando fala)"
                                    },
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
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

                // Cápsula de Entrada de Mensagem
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color(0xFF212121))
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Botão "+" (Abre BottomSheet com as ferramentas do app)
                    IconButton(
                        onClick = { showToolsSheet = true },
                        modifier = Modifier.size(38.dp)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Opções e Ferramentas",
                            tint = Color.White,
                            modifier = Modifier.size(23.dp)
                        )
                    }

                    // Campo de Texto Transparente "Responder ao ChatGPT"
                    BasicTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        textStyle = TextStyle(
                            color = Color.White,
                            fontSize = 15.5.sp
                        ),
                        cursorBrush = SolidColor(Color.White),
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
                                    text = if (isListeningSpeech) "A ouvir voz em tempo real..." else "Responder ao ChatGPT",
                                    color = if (isListeningSpeech) Color(0xFF38BDF8) else Color(0xFF8E8E93),
                                    fontSize = 15.5.sp
                                )
                            }
                            innerTextField()
                        }
                    )

                    if (inputText.isNotBlank()) {
                        // Botão Azul de Enviar quando o usuário digita
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF2563EB))
                                .clickable {
                                    executeAction(AssistantActionType.ASK, inputText, isLiveConversationMode)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Enviar Mensagem",
                                tint = Color.White,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    } else {
                        // Estado inicial sem texto: Ícone do Microfone + Botão Azul com Ondas Sonoras (Live)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    if (isListeningSpeech) {
                                        try {
                                            speechRecognizer?.stopListening()
                                        } catch (_: Exception) {}
                                        isListeningSpeech = false
                                        Toast.makeText(context, "Gravação finalizada", Toast.LENGTH_SHORT).show()
                                    } else {
                                        startListening(false)
                                    }
                                },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(
                                    Icons.Default.Mic,
                                    contentDescription = if (isListeningSpeech) "Parar gravação" else "Entrada de Voz",
                                    tint = if (isListeningSpeech) Color(0xFF38BDF8) else Color(0xFFE5E7EB),
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2563EB))
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
                                VoiceWaveformIcon(isLive = isLiveConversationMode)
                            }
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

/**
 * Modelo de Mensagem de Conversa para a interface
 */
data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isUser: Boolean,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val result: AssistantResult? = null
)

/**
 * Indicador animado minimalista de três pontos a balançar verticalmente (●  ●  ●).
 * - Movimentos verticais alternados e dessincronizados de forma suave e contínua.
 * - Leve, eficiente para Android e consumo de bateria.
 * - Totalmente sem textos desnecessários.
 */
@Composable
fun TypingBouncingDots(
    modifier: Modifier = Modifier,
    dotSize: Dp = 6.5.dp,
    dotColor: Color = Color(0xFFE5E7EB),
    spacing: Dp = 6.dp,
    travelDistance: Dp = 5.5.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "bouncing_dots_transition")
    val delays = listOf(0, 160, 320)

    val animations = delays.mapIndexed { index, delayMillis ->
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 0f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 1100
                    0f at 0 using FastOutSlowInEasing
                    -1f at 280 using FastOutSlowInEasing
                    0f at 560 using FastOutSlowInEasing
                    0f at 1100 using LinearEasing
                },
                repeatMode = RepeatMode.Restart,
                initialStartOffset = StartOffset(delayMillis)
            ),
            label = "dot_offset_$index"
        )
    }

    Row(
        modifier = modifier.padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        animations.forEach { anim ->
            Box(
                modifier = Modifier
                    .offset(y = travelDistance * anim.value)
                    .size(dotSize)
                    .clip(CircleShape)
                    .background(dotColor)
            )
        }
    }
}

/**
 * Ícone com 4 barras de ondas sonoras verticais (||||) do botão Live azul
 */
@Composable
fun VoiceWaveformIcon(
    isLive: Boolean = false,
    modifier: Modifier = Modifier
) {
    if (!isLive) {
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(2.2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.width(2.dp).height(6.5.dp).background(Color.White, RoundedCornerShape(1.dp)))
            Box(modifier = Modifier.width(2.dp).height(16.dp).background(Color.White, RoundedCornerShape(1.dp)))
            Box(modifier = Modifier.width(2.dp).height(11.dp).background(Color.White, RoundedCornerShape(1.dp)))
            Box(modifier = Modifier.width(2.dp).height(5.dp).background(Color.White, RoundedCornerShape(1.dp)))
        }
    } else {
        val transition = rememberInfiniteTransition(label = "wave_pulsing")
        val h1 by transition.animateFloat(
            initialValue = 6f, targetValue = 14f,
            animationSpec = infiniteRepeatable(tween(400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "h1"
        )
        val h2 by transition.animateFloat(
            initialValue = 17f, targetValue = 8f,
            animationSpec = infiniteRepeatable(tween(450, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "h2"
        )
        val h3 by transition.animateFloat(
            initialValue = 10f, targetValue = 18f,
            animationSpec = infiniteRepeatable(tween(380, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "h3"
        )
        val h4 by transition.animateFloat(
            initialValue = 5f, targetValue = 12f,
            animationSpec = infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "h4"
        )
        Row(
            modifier = modifier,
            horizontalArrangement = Arrangement.spacedBy(2.2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.width(2.dp).height(h1.dp).background(Color.White, RoundedCornerShape(1.dp)))
            Box(modifier = Modifier.width(2.dp).height(h2.dp).background(Color.White, RoundedCornerShape(1.dp)))
            Box(modifier = Modifier.width(2.dp).height(h3.dp).background(Color.White, RoundedCornerShape(1.dp)))
            Box(modifier = Modifier.width(2.dp).height(h4.dp).background(Color.White, RoundedCornerShape(1.dp)))
        }
    }
}

/**
 * Balão de mensagem do Utilizador (Azul Escuro, alinhado à direita, cantos arredondados suaves)
 */
@Composable
fun UserMessageItem(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 310.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF0C1968))
                .padding(horizontal = 18.dp, vertical = 11.dp)
        ) {
            Text(
                text = text,
                color = Color.White,
                fontSize = 15.5.sp,
                lineHeight = 22.sp
            )
        }
    }
}

/**
 * Mensagem da IA / Assistente (Diretamente sobre fundo preto, sem balão, tipografia limpa e barra de ações)
 */
@Composable
fun AssistantMessageItem(
    message: ChatMessage,
    isTtsSpeaking: Boolean,
    onSpeak: () -> Unit,
    onStopTts: () -> Unit,
    onOpenNotes: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var liked by remember { mutableStateOf<Boolean?>(null) }
    var showMoreMenu by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        // Texto da resposta com markdown e tipografia fiel à referência
        Text(
            text = parseMarkdownText(message.text),
            color = Color(0xFFF9FAFB),
            fontSize = 15.5.sp,
            lineHeight = 23.sp
        )

        // Diagrama Mermaid se houver
        message.result?.diagramCode?.takeIf { it.isNotBlank() }?.let { diagram ->
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16181F)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Diagrama Mermaid", color = Color(0xFFA855F7), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = diagram,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFFA7F3D0)
                    )
                }
            }
        }

        // Imagem IA gerada se houver
        message.result?.imageUrl?.takeIf { it.isNotBlank() }?.let { imgUrl ->
            Spacer(modifier = Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF16181F)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column {
                    AsyncNetworkImage(
                        url = imgUrl,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Barra de Ações (Fiel à imagem de referência: Copiar, Like, Dislike, Ouvir, Partilhar, Mais)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Copiar
            IconButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(cleanMarkdown(message.text)))
                    Toast.makeText(context, "Copiado para a área de transferência", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(26.dp)
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Copiar",
                    tint = Color(0xFF9CA3AF),
                    modifier = Modifier.size(17.dp)
                )
            }

            // Like / Thumbs Up
            IconButton(
                onClick = {
                    liked = if (liked == true) null else true
                    if (liked == true) Toast.makeText(context, "Obrigado pelo feedback!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(26.dp)
            ) {
                Icon(
                    if (liked == true) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                    contentDescription = "Gostei",
                    tint = if (liked == true) Color(0xFF38BDF8) else Color(0xFF9CA3AF),
                    modifier = Modifier.size(17.dp)
                )
            }

            // Dislike / Thumbs Down
            IconButton(
                onClick = {
                    liked = if (liked == false) null else false
                    if (liked == false) Toast.makeText(context, "Feedback registado", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(26.dp)
            ) {
                Icon(
                    if (liked == false) Icons.Filled.ThumbDown else Icons.Outlined.ThumbDown,
                    contentDescription = "Não gostei",
                    tint = if (liked == false) Color(0xFFF87171) else Color(0xFF9CA3AF),
                    modifier = Modifier.size(17.dp)
                )
            }

            // Ouvir por voz / TTS
            IconButton(
                onClick = {
                    if (isTtsSpeaking) onStopTts() else onSpeak()
                },
                modifier = Modifier.size(26.dp)
            ) {
                Icon(
                    if (isTtsSpeaking) Icons.Default.Stop else Icons.Default.VolumeUp,
                    contentDescription = "Ouvir",
                    tint = if (isTtsSpeaking) Color(0xFF38BDF8) else Color(0xFF9CA3AF),
                    modifier = Modifier.size(17.dp)
                )
            }

            // Partilhar
            IconButton(
                onClick = {
                    try {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, cleanMarkdown(message.text))
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Partilhar resposta"))
                    } catch (_: Exception) {}
                },
                modifier = Modifier.size(26.dp)
            ) {
                Icon(
                    Icons.Default.Share,
                    contentDescription = "Partilhar",
                    tint = Color(0xFF9CA3AF),
                    modifier = Modifier.size(17.dp)
                )
            }

            // Mais opções
            Box {
                IconButton(
                    onClick = { showMoreMenu = true },
                    modifier = Modifier.size(26.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Mais opções",
                        tint = Color(0xFF9CA3AF),
                        modifier = Modifier.size(17.dp)
                    )
                }
                DropdownMenu(
                    expanded = showMoreMenu,
                    onDismissRequest = { showMoreMenu = false },
                    containerColor = Color(0xFF1E2128)
                ) {
                    DropdownMenuItem(
                        text = { Text("Copiar texto", color = Color.White, fontSize = 13.sp) },
                        onClick = {
                            showMoreMenu = false
                            clipboardManager.setText(AnnotatedString(cleanMarkdown(message.text)))
                            Toast.makeText(context, "Copiado!", Toast.LENGTH_SHORT).show()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Guardar nas Notas", color = Color.White, fontSize = 13.sp) },
                        onClick = {
                            showMoreMenu = false
                            onOpenNotes()
                        }
                    )
                }
            }
        }
    }
}

/**
 * Converte marcação markdown com **negrito** para AnnotatedString estilizada com FontWeight.Bold
 */
fun parseMarkdownText(text: String): AnnotatedString {
    return buildAnnotatedString {
        val parts = text.split("**")
        for (i in parts.indices) {
            if (i % 2 == 1) {
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color.White))
                append(parts[i])
                pop()
            } else {
                append(parts[i])
            }
        }
    }
}

/**
 * Remove caracteres de markdown para leitura fluida de TTS e cópia simples
 */
fun cleanMarkdown(text: String): String {
    return text.replace(Regex("""[#*`_]"""), "").trim()
}
