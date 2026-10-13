package com.example.aiquestionassistant.ui.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiquestionassistant.data.AppSettings
import com.example.aiquestionassistant.service.FloatingBubbleService
import java.util.Locale

data class ModelOption(
    val id: String,
    val name: String,
    val subtitle: String,
    val badge: String,
    val icon: ImageVector
)

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var settings by remember { mutableStateOf(AppSettings.load(context)) }
    var hasOverlayPermission by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var isServiceRunning by remember { mutableStateOf(FloatingBubbleService.isRunning) }

    var showGeminiKey by remember { mutableStateOf(false) }
    var showAgentRouterKey by remember { mutableStateOf(false) }
    var showOpenRouterKey by remember { mutableStateOf(false) }

    // TextToSpeech para testar a voz
    var tts by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        val ttsInstance = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("pt", "BR")
            }
        }
        tts = ttsInstance
        onDispose {
            ttsInstance.stop()
            ttsInstance.shutdown()
        }
    }

    val availableModels = listOf(
        ModelOption(
            id = "gemini-2.5-flash",
            name = "Gemini Flash",
            subtitle = "Rápido, multimodal e equilibrado",
            badge = "Recomendado",
            icon = Icons.Default.AutoAwesome
        ),
        ModelOption(
            id = "claude-opus-4-8",
            name = "Claude Opus 4.8",
            subtitle = "Raciocínio extremo, exames e síntese profunda via AgentRouter",
            badge = "AgentRouter",
            icon = Icons.Default.Psychology
        ),
        ModelOption(
            id = "claude-opus-5",
            name = "Claude Opus 5",
            subtitle = "Nova geração Anthropic de máxima precisão lógica via AgentRouter",
            badge = "AgentRouter",
            icon = Icons.Default.AutoAwesome
        ),
        ModelOption(
            id = "deepseek-v4-flash",
            name = "DeepSeek V4 Flash",
            subtitle = "Ultrarrápido, menor latência para respostas ágeis via AgentRouter",
            badge = "AgentRouter",
            icon = Icons.Default.Bolt
        ),
        ModelOption(
            id = "gpt-6-astra",
            name = "GPT-6 Astra",
            subtitle = "Multimodal e raciocínio profundo OpenAI via AgentRouter",
            badge = "AgentRouter",
            icon = Icons.Default.AutoAwesome
        ),
        ModelOption(
            id = "deepseek/deepseek-r1:free",
            name = "DeepSeek R1",
            subtitle = "Raciocínio lógico e matemático via OpenRouter",
            badge = "Open Source",
            icon = Icons.Default.Tune
        ),
        ModelOption(
            id = "gemini-2.5-pro",
            name = "Gemini Pro",
            subtitle = "Raciocínio avançado, código e exames",
            badge = "Alta Precisão",
            icon = Icons.Default.Psychology
        ),
        ModelOption(
            id = "gemini-2.0-flash-lite",
            name = "TypeSafe JEV",
            subtitle = "Ultra-rápido, respostas concisas e diretas",
            badge = "Velocidade Máxima",
            icon = Icons.Default.Bolt
        )
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF0A0C10),
                        Color(0xFF0F1117),
                        Color(0xFF13151D)
                    )
                )
            )
    ) {
        // TopBar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Voltar",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = "Definições",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Configuração de IA, Servidor e Botão",
                    color = Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }
        }

        HorizontalDivider(color = Color(0xFF222632))

        // Conteúdo Rolável
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // =========================================================================
            // 1. MODELO DE IA (A PARTE MARCADA PELO USUÁRIO)
            // =========================================================================
            SettingsSectionHeader(
                title = "Modelo de Inteligência Artificial",
                subtitle = "Escolha o modelo de IA para responder e processar consultas",
                icon = Icons.Default.AutoAwesome
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                availableModels.forEach { modelOpt ->
                    val isSelected = settings.model.equals(modelOpt.id, ignoreCase = true) ||
                            (modelOpt.name == "Gemini Flash" && (settings.model == "Gemini Flash" || settings.model.isBlank())) ||
                            (modelOpt.name == "Gemini Pro" && settings.model == "Gemini Pro") ||
                            (modelOpt.name == "TypeSafe JEV" && settings.model == "TypeSafe JEV") ||
                            (modelOpt.id == "claude-opus-4-8" && settings.model.equals("laude-opus-4-8", ignoreCase = true))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                settings = settings.copy(model = modelOpt.id)
                            }
                            .then(
                                if (isSelected) {
                                    Modifier.border(1.5.dp, Color(0xFF38BDF8), RoundedCornerShape(14.dp))
                                } else {
                                    Modifier.border(1.dp, Color(0xFF232733), RoundedCornerShape(14.dp))
                                }
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF182232) else Color(0xFF171922)
                        ),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) Color(0xFF0284C7).copy(alpha = 0.25f)
                                        else Color(0xFF252936)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = modelOpt.icon,
                                    contentDescription = null,
                                    tint = if (isSelected) Color(0xFF38BDF8) else Color.Gray,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = modelOpt.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isSelected) Color(0xFF0369A1)
                                                else Color(0xFF2A2E3D)
                                            )
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = modelOpt.badge,
                                            color = if (isSelected) Color.White else Color(0xFF94A3B8),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Text(
                                    text = modelOpt.subtitle,
                                    color = Color(0xFF94A3B8),
                                    fontSize = 12.sp
                                )
                            }

                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF38BDF8)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selecionado",
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 2. CONEXÃO DO SERVIDOR & CHAVES DE API
            // =========================================================================
            SettingsSectionHeader(
                title = "Servidor e Chaves de API",
                subtitle = "Conecte o backend local ou use chaves de API diretas",
                icon = Icons.Default.Dns
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF171922)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF232733)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = settings.serverUrl,
                        onValueChange = { settings = settings.copy(serverUrl = it) },
                        label = { Text("URL do Servidor Backend", color = Color(0xFF94A3B8)) },
                        placeholder = { Text("http://10.0.2.2:3001 ou IP da sua rede", color = Color.DarkGray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF2E3344)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = settings.directGeminiKey,
                        onValueChange = { settings = settings.copy(directGeminiKey = it) },
                        label = { Text("Chave Direta Google Gemini (Opcional)", color = Color(0xFF94A3B8)) },
                        placeholder = { Text("AIzaSy...", color = Color.DarkGray) },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (showGeminiKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showGeminiKey = !showGeminiKey }) {
                                Icon(
                                    imageVector = if (showGeminiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = Color.Gray
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF2E3344)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = settings.directAgentRouterKey,
                        onValueChange = { settings = settings.copy(directAgentRouterKey = it) },
                        label = { Text("Chave Direta AgentRouter (agentrouter.org)", color = Color(0xFF94A3B8)) },
                        placeholder = { Text("Cole sua chave da API AgentRouter...", color = Color.DarkGray) },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (showAgentRouterKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showAgentRouterKey = !showAgentRouterKey }) {
                                Icon(
                                    imageVector = if (showAgentRouterKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = Color.Gray
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF2E3344)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = settings.directOpenRouterKey,
                        onValueChange = { settings = settings.copy(directOpenRouterKey = it) },
                        label = { Text("Chave Direta OpenRouter (Opcional)", color = Color(0xFF94A3B8)) },
                        placeholder = { Text("sk-or-v1-...", color = Color.DarkGray) },
                        modifier = Modifier.fillMaxWidth(),
                        visualTransformation = if (showOpenRouterKey) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showOpenRouterKey = !showOpenRouterKey }) {
                                Icon(
                                    imageVector = if (showOpenRouterKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = Color.Gray
                                )
                            }
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF2E3344)
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )
                }
            }

            // =========================================================================
            // 3. BOTÃO FLUTUANTE (BUBBLE)
            // =========================================================================
            SettingsSectionHeader(
                title = "Botão Flutuante (Overlay Bubble)",
                subtitle = "Assistente sempre visível sobre outras aplicações",
                icon = Icons.Default.Layers
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF171922)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF232733)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Switch de Ativação
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ativar Botão Flutuante",
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = if (isServiceRunning) "Em execução sobre outros aplicativos" else "Desativado",
                                color = if (isServiceRunning) Color(0xFF34D399) else Color(0xFF94A3B8),
                                fontSize = 12.sp
                            )
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
                                uncheckedThumbColor = Color.LightGray,
                                uncheckedTrackColor = Color(0xFF2E3344)
                            )
                        )
                    }

                    HorizontalDivider(color = Color(0xFF232733))

                    // Slider de Ajuste de Tamanho com Preview Dinâmico
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tamanho do Botão Flutuante",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "${settings.bubbleSize} dp",
                                color = Color(0xFF38BDF8),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Slider(
                                value = settings.bubbleSize.toFloat(),
                                onValueChange = {
                                    val newSize = it.toInt()
                                    settings = settings.copy(bubbleSize = newSize)
                                    FloatingBubbleService.instance?.updateBubbleSize(newSize)
                                },
                                valueRange = 40f..84f,
                                modifier = Modifier.weight(1f),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF38BDF8),
                                    activeTrackColor = Color(0xFF38BDF8),
                                    inactiveTrackColor = Color(0xFF2A2E3D)
                                )
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            // Preview Circular do Botão
                            Box(
                                modifier = Modifier
                                    .size((settings.bubbleSize * 0.7f).dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Color(0xFF6366F1), Color(0xFF38BDF8))
                                        )
                                    )
                                    .border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 4. PREFERÊNCIAS DE RESPOSTA E ÁUDIO
            // =========================================================================
            SettingsSectionHeader(
                title = "Preferências e Respostas",
                subtitle = "Personalize o comportamento do assistente e a voz",
                icon = Icons.Default.Tune
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF171922)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF232733)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Cópia Automática
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Cópia Automática", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text("Copiar respostas automaticamente para a área de transferência", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                        Switch(
                            checked = settings.autoCopy,
                            onCheckedChange = { settings = settings.copy(autoCopy = it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF38BDF8)
                            )
                        )
                    }

                    HorizontalDivider(color = Color(0xFF232733))

                    // Vibração Háptica
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Vibração Háptica", color = Color.White, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text("Vibrar levemente ao concluir análise de IA", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        }
                        Switch(
                            checked = settings.hapticFeedback,
                            onCheckedChange = { settings = settings.copy(hapticFeedback = it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF38BDF8)
                            )
                        )
                    }

                    HorizontalDivider(color = Color(0xFF232733))

                    // Velocidade TTS
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Velocidade da Voz (TTS)", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(String.format(Locale.US, "%.1fx", settings.ttsSpeechRate), color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = settings.ttsSpeechRate,
                            onValueChange = { settings = settings.copy(ttsSpeechRate = it) },
                            valueRange = 0.5f..2.0f,
                            colors = SliderDefaults.colors(
                                thumbColor = Color(0xFF38BDF8),
                                activeTrackColor = Color(0xFF38BDF8),
                                inactiveTrackColor = Color(0xFF2A2E3D)
                            )
                        )
                    }
                }
            }

            // =========================================================================
            // 5. PERFIL DO USUÁRIO
            // =========================================================================
            SettingsSectionHeader(
                title = "Perfil do Utilizador",
                subtitle = "Informações do utilizador ativo",
                icon = Icons.Default.Person
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF171922)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF232733)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
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
                            fontSize = 22.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text("Cláudio", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text("Utilizador Principal • Local & Servidor", color = Color(0xFF94A3B8), fontSize = 12.sp)
                        Text("Versão 1.0.0 (Gemini 2.5 Hybrid)", color = Color(0xFF38BDF8), fontSize = 11.sp)
                    }
                }
            }

            // =========================================================================
            // BOTÃO SALVAR DEFINIÇÕES
            // =========================================================================
            Button(
                onClick = {
                    AppSettings.save(context, settings)
                    FloatingBubbleService.instance?.updateBubbleSize(settings.bubbleSize)
                    Toast.makeText(context, "Definições guardadas com sucesso!", Toast.LENGTH_SHORT).show()
                    onBack()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Guardar Definições",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    title: String,
    subtitle: String,
    icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1E2433)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF38BDF8),
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
            Text(
                text = subtitle,
                color = Color(0xFF94A3B8),
                fontSize = 11.sp
            )
        }
    }
}
