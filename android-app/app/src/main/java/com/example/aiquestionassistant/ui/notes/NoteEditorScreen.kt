package com.example.aiquestionassistant.ui.notes

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.FormatUnderlined
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Title
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiquestionassistant.data.AiRepository
import com.example.aiquestionassistant.data.AppSettings
import com.example.aiquestionassistant.data.Note
import com.example.aiquestionassistant.data.NoteAiAction
import com.example.aiquestionassistant.data.NoteCategory
import com.example.aiquestionassistant.data.NoteColor
import com.example.aiquestionassistant.data.NoteTag
import com.example.aiquestionassistant.data.NotesLocalRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NoteEditorScreen(
    noteId: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { NotesLocalRepository(context) }
    val aiRepo = remember { AiRepository() }
    val settings = remember { AppSettings.load(context) }

    // Estados da Nota
    var currentNoteId by remember { mutableStateOf(noteId ?: java.util.UUID.randomUUID().toString()) }
    var title by remember { mutableStateOf("") }
    var contentValue by remember { mutableStateOf(TextFieldValue("")) }
    var customColorHex by remember { mutableStateOf("#2e80ea") }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var tags by remember { mutableStateOf<List<String>>(emptyList()) }
    var isPinned by remember { mutableStateOf(false) }
    var isFavorite by remember { mutableStateOf(false) }
    var createdAt by remember { mutableStateOf(System.currentTimeMillis()) }
    var updatedAt by remember { mutableStateOf(System.currentTimeMillis()) }

    // Estados de salvamento
    var saveStatus by remember { mutableStateOf("Guardado") } // "A guardar...", "Guardado", "Erro ao guardar"
    var isInitialLoaded by remember { mutableStateOf(false) }

    // Categorias e Tags cadastradas
    var categories by remember { mutableStateOf<List<NoteCategory>>(emptyList()) }
    var availableTags by remember { mutableStateOf<List<NoteTag>>(emptyList()) }

    // Sheets e Diálogos
    var showAiSheet by remember { mutableStateOf(false) }
    var showColorPaletteSheet by remember { mutableStateOf(false) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var newTagInput by remember { mutableStateOf("") }

    // Estado de Pré-visualização da IA (Requisito 5: NUNCA substituir silenciosamente)
    var aiPreviewResult by remember { mutableStateOf<Pair<NoteAiAction, String>?>(null) }
    var isAiProcessing by remember { mutableStateOf(false) }

    // Carregar Nota existente
    LaunchedEffect(noteId) {
        categories = repo.getCategories()
        availableTags = repo.getTags()
        if (!noteId.isNullOrBlank()) {
            val loaded = repo.getNoteById(noteId)
            if (loaded != null) {
                currentNoteId = loaded.id
                title = loaded.title
                contentValue = TextFieldValue(loaded.content)
                val rawCol = loaded.color.trim()
                customColorHex = if (rawCol.startsWith("#")) rawCol else {
                    NoteColor.entries.find { it.name.equals(rawCol, ignoreCase = true) }?.hex ?: "#2e80ea"
                }
                selectedCategoryId = loaded.categoryId
                tags = loaded.tags
                isPinned = loaded.isPinned
                isFavorite = loaded.isFavorite
                createdAt = loaded.createdAt
                updatedAt = loaded.updatedAt
            }
        }
        isInitialLoaded = true
    }

    // Auto-save com debounce de 600ms
    LaunchedEffect(title, contentValue.text, customColorHex, selectedCategoryId, tags, isPinned, isFavorite) {
        if (!isInitialLoaded) return@LaunchedEffect
        saveStatus = "A guardar..."
        delay(600)
        try {
            val toSave = Note(
                id = currentNoteId,
                title = title,
                content = contentValue.text,
                color = customColorHex,
                categoryId = selectedCategoryId,
                tags = tags,
                isPinned = isPinned,
                isFavorite = isFavorite,
                createdAt = createdAt,
                updatedAt = System.currentTimeMillis()
            )
            repo.saveNote(toSave)
            updatedAt = System.currentTimeMillis()
            saveStatus = "Guardado"
        } catch (_: Exception) {
            saveStatus = "Erro ao guardar"
        }
    }

    // Cálculos de palavras e caracteres
    val charCount = contentValue.text.length
    val wordCount = if (contentValue.text.isBlank()) 0 else contentValue.text.trim().split("\\s+".toRegex()).size
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }

    val parsedCardColor = remember(customColorHex) {
        try {
            Color(android.graphics.Color.parseColor(customColorHex))
        } catch (_: Exception) {
            Color(0xFF2E80EA)
        }
    }

    val isEditorLight = remember(parsedCardColor) {
        val r = (parsedCardColor.red * 255).toInt()
        val g = (parsedCardColor.green * 255).toInt()
        val b = (parsedCardColor.blue * 255).toInt()
        ((r * 299) + (g * 587) + (b * 114)) / 1000 >= 128
    }

    val editorTextColor = if (isEditorLight) Color(0xFF111827) else Color.White
    val editorSubTextColor = if (isEditorLight) Color(0xFF374151) else Color(0xFFE2E8F0)

    Surface(
        modifier = modifier.fillMaxSize(),
        color = parsedCardColor
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TOP BAR DO EDITOR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF18181B))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = saveStatus,
                        color = when (saveStatus) {
                            "Guardado" -> Color(0xFF34D399)
                            "Erro ao guardar" -> Color(0xFFF87171)
                            else -> Color(0xFFFBBF24)
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    // Botão Assistente de IA
                    Button(
                        onClick = { showAiSheet = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF212124)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Assistente IA", color = Color(0xFF38BDF8), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Paleta de Cores
                    IconButton(onClick = { showColorPaletteSheet = true }) {
                        Icon(Icons.Default.ColorLens, contentDescription = "Cor", tint = Color.LightGray)
                    }

                    // Fixar
                    IconButton(onClick = { isPinned = !isPinned }) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = "Fixar",
                            tint = if (isPinned) Color(0xFFFBBF24) else Color.Gray
                        )
                    }

                    // Favorito
                    IconButton(onClick = { isFavorite = !isFavorite }) {
                        Icon(
                            if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favoritar",
                            tint = if (isFavorite) Color(0xFFF472B6) else Color.Gray
                        )
                    }
                }
            }

            // BARRA DE FORMATAÇÃO RICA RÁPIDA (Markdown / Negrito / Listas / etc.)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF141416))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Negrito
                IconButton(onClick = {
                    val sel = contentValue.selection
                    val text = contentValue.text
                    val before = text.substring(0, sel.start)
                    val selected = text.substring(sel.start, sel.end)
                    val after = text.substring(sel.end)
                    val newText = "$before**$selected**$after"
                    contentValue = TextFieldValue(newText, TextRange(sel.start + 2, sel.end + 2))
                }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.FormatBold, contentDescription = "Negrito", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                }

                // Itálico
                IconButton(onClick = {
                    val sel = contentValue.selection
                    val text = contentValue.text
                    val before = text.substring(0, sel.start)
                    val selected = text.substring(sel.start, sel.end)
                    val after = text.substring(sel.end)
                    val newText = "$before*$selected*$after"
                    contentValue = TextFieldValue(newText, TextRange(sel.start + 1, sel.end + 1))
                }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.FormatItalic, contentDescription = "Itálico", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                }

                // Título (Heading)
                IconButton(onClick = {
                    val sel = contentValue.selection
                    val text = contentValue.text
                    val before = text.substring(0, sel.start)
                    val after = text.substring(sel.start)
                    val newText = "$before## $after"
                    contentValue = TextFieldValue(newText, TextRange(sel.start + 3))
                }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Title, contentDescription = "Título", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                }

                // Lista com marcadores
                IconButton(onClick = {
                    val sel = contentValue.selection
                    val text = contentValue.text
                    val before = text.substring(0, sel.start)
                    val after = text.substring(sel.start)
                    val newText = "$before\n- $after"
                    contentValue = TextFieldValue(newText, TextRange(sel.start + 3))
                }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.FormatListBulleted, contentDescription = "Lista", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                }

                // Lista de Tarefas (Checklist)
                IconButton(onClick = {
                    val sel = contentValue.selection
                    val text = contentValue.text
                    val before = text.substring(0, sel.start)
                    val after = text.substring(sel.start)
                    val newText = "$before\n- [ ] $after"
                    contentValue = TextFieldValue(newText, TextRange(sel.start + 7))
                }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Checklist, contentDescription = "Checklist", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                }

                Spacer(modifier = Modifier.weight(1f))

                // Categoria e Tags seletores
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2563EB).copy(alpha = 0.25f))
                        .clickable { showCategorySheet = true }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    val catName = categories.find { it.id == selectedCategoryId }?.name ?: "Sem Categoria"
                    Text(catName, color = Color(0xFF93C5FD), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }

                IconButton(onClick = { showTagDialog = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Label, contentDescription = "Tags", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                }
            }

            // TAGS ATIVAS NA NOTA
            if (tags.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    tags.forEach { t ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF262933))
                                .padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("#$t", color = Color(0xFFE2E8F0), fontSize = 11.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Clear,
                                contentDescription = "Remover",
                                tint = Color.Gray,
                                modifier = Modifier.size(12.dp).clickable { tags = tags.filter { it != t } }
                            )
                        }
                    }
                }
            }

            // CORPO DO EDITOR (Título + Conteúdo)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // TÍTULO DA NOTA
                BasicTextField(
                    value = title,
                    onValueChange = { title = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(
                        color = editorTextColor,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    decorationBox = { inner ->
                        if (title.isEmpty()) {
                            Text("Título da nota", color = editorSubTextColor.copy(alpha = 0.6f), fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        }
                        inner()
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))

                // CONTEÚDO DA NOTA
                BasicTextField(
                    value = contentValue,
                    onValueChange = { contentValue = it },
                    modifier = Modifier.fillMaxWidth().height(450.dp),
                    textStyle = TextStyle(
                        color = editorSubTextColor,
                        fontSize = 15.sp,
                        lineHeight = 22.sp
                    ),
                    decorationBox = { inner ->
                        if (contentValue.text.isEmpty()) {
                            Text("Escreva a sua nota aqui...", color = editorSubTextColor.copy(alpha = 0.6f), fontSize = 15.sp)
                        }
                        inner()
                    }
                )
            }

            // RODAPÉ: Contagem de Palavras, Caracteres e Datas
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF111319))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$wordCount palavras  •  $charCount caracteres",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
                Text(
                    text = "Editado em ${dateFormat.format(Date(updatedAt))}",
                    color = Color.Gray,
                    fontSize = 11.sp
                )
            }
        }
    }

    // MODAL BOTTOM SHEET: ASSISTENTE DE IA (10 AÇÕES DO REQUISITO 5)
    if (showAiSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAiSheet = false },
            containerColor = Color(0xFF16181D)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF38BDF8))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ações do Assistente de IA", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                val selRange = contentValue.selection
                val selText = if (selRange.start != selRange.end) {
                    val start = kotlin.math.min(selRange.start, selRange.end)
                    val end = kotlin.math.max(selRange.start, selRange.end)
                    contentValue.text.substring(start, end)
                } else ""
                Text(
                    text = if (selText.isNotBlank()) "A aplicar sobre o trecho selecionado (${selText.length} carateres)" else "A aplicar sobre a nota inteira",
                    color = Color(0xFF38BDF8),
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(14.dp))

                val aiActions = listOf(
                    NoteAiAction.SUMMARIZE,
                    NoteAiAction.ORGANIZE,
                    NoteAiAction.IMPROVE,
                    NoteAiAction.EXPAND,
                    NoteAiAction.KEYPOINTS,
                    NoteAiAction.CHECKLIST,
                    NoteAiAction.REVIEW_QUESTIONS,
                    NoteAiAction.SUGGEST_TITLE,
                    NoteAiAction.SUGGEST_TAXONOMY,
                    NoteAiAction.GENERATE
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    aiActions.forEach { action ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showAiSheet = false
                                    isAiProcessing = true
                                    scope.launch {
                                        val (ok, res) = aiRepo.executeNoteAction(
                                            settings = settings,
                                            action = action,
                                            topic = title.ifBlank { "Apontamentos" },
                                            content = contentValue.text,
                                            selection = if (selText.isNotBlank()) selText else null
                                        )
                                        isAiProcessing = false
                                        if (ok && res.isNotBlank()) {
                                            aiPreviewResult = Pair(action, res)
                                        } else {
                                            Toast.makeText(context, "Falha ao processar ação de IA", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1F222A)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(action.label, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(action.description, color = Color.Gray, fontSize = 12.sp)
                                }
                                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // INDICADOR DE PROCESSAMENTO DA IA
    if (isAiProcessing) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = Color(0xFF1E2026),
            title = { Text("Assistente de IA a trabalhar...", color = Color.White) },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(color = Color(0xFF38BDF8), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("A processar a sua solicitação com inteligência artificial...", color = Color.LightGray, fontSize = 13.sp)
                }
            },
            confirmButton = {}
        )
    }

    // DIÁLOGO DE PRÉ-VISUALIZAÇÃO DA IA (REQUISITO 5: NUNCA SUBSTITUIR SILENCIOSAMENTE)
    aiPreviewResult?.let { (action, resultText) ->
        AlertDialog(
            onDismissRequest = { aiPreviewResult = null },
            containerColor = Color(0xFF1A1C23),
            title = {
                Text("Pré-visualização: ${action.label}", color = Color(0xFF38BDF8), fontSize = 17.sp, fontWeight = FontWeight.Bold)
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text("Resultado gerado pela IA:", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF111317)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = resultText,
                            color = Color(0xFFE2E8F0),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Substituir
                    Button(
                        onClick = {
                            if (action == NoteAiAction.SUGGEST_TITLE) {
                                title = resultText.trim().removeSurrounding("\"").removeSurrounding("'").trim()
                            } else {
                                val sel = contentValue.selection
                                if (sel.start != sel.end) {
                                    val text = contentValue.text
                                    val before = text.substring(0, sel.start)
                                    val after = text.substring(sel.end)
                                    contentValue = TextFieldValue("$before$resultText$after")
                                } else {
                                    contentValue = TextFieldValue(resultText)
                                }
                            }
                            aiPreviewResult = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Text("Substituir", color = Color.White, fontSize = 12.sp)
                    }

                    // Inserir no final
                    Button(
                        onClick = {
                            val current = contentValue.text
                            val newText = if (current.isBlank()) resultText else "$current\n\n$resultText"
                            contentValue = TextFieldValue(newText)
                            aiPreviewResult = null
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                    ) {
                        Text("Inserir", color = Color.White, fontSize = 12.sp)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { aiPreviewResult = null }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }

    // PALETA VISUAL DE CORES (5 CORES SOLICITADAS + GERADOR RGB)
    if (showColorPaletteSheet) {
        var rgbR by remember { mutableStateOf(46f) }
        var rgbG by remember { mutableStateOf(128f) }
        var rgbB by remember { mutableStateOf(234f) }

        ModalBottomSheet(
            onDismissRequest = { showColorPaletteSheet = false },
            containerColor = Color(0xFF1E2026)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text("Personalizar Cor do Bloco", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))

                // 1. 5 Cores Principais do Card (#2e80ea, #eeff00, #82ff80, #ffd91a, #bdbdbd)
                Text("Cores Predefinidas", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    NoteColor.entries.forEach { colorEnum ->
                        val isSelected = customColorHex.equals(colorEnum.hex, ignoreCase = true)
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable {
                                customColorHex = colorEnum.hex
                                showColorPaletteSheet = false
                            }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(colorEnum.hex)))
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) Color.White else Color.Black.copy(alpha = 0.3f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(colorEnum.label, color = Color.LightGray, fontSize = 11.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = Color(0xFF2E323D))
                Spacer(modifier = Modifier.height(14.dp))

                // 2. Gerador Dinâmico RGB de Cores
                Text("Gerador Personalizado via RGB", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(10.dp))

                val generatedColor = Color(rgbR.toInt(), rgbG.toInt(), rgbB.toInt())
                val generatedHex = String.format("#%02x%02x%02x", rgbR.toInt(), rgbG.toInt(), rgbB.toInt())

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Preview da cor gerada
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(generatedColor)
                            .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = generatedHex.uppercase(),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "RGB(${rgbR.toInt()}, ${rgbG.toInt()}, ${rgbB.toInt()})",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                    Button(
                        onClick = {
                            customColorHex = generatedHex
                            showColorPaletteSheet = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Aplicar", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Slider Vermelho (R)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("R: ${rgbR.toInt()}", color = Color(0xFFF87171), fontSize = 12.sp, modifier = Modifier.width(52.dp), fontWeight = FontWeight.Bold)
                    androidx.compose.material3.Slider(
                        value = rgbR,
                        onValueChange = { rgbR = it },
                        valueRange = 0f..255f,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Slider Verde (G)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("G: ${rgbG.toInt()}", color = Color(0xFF4ADE80), fontSize = 12.sp, modifier = Modifier.width(52.dp), fontWeight = FontWeight.Bold)
                    androidx.compose.material3.Slider(
                        value = rgbG,
                        onValueChange = { rgbG = it },
                        valueRange = 0f..255f,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Slider Azul (B)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("B: ${rgbB.toInt()}", color = Color(0xFF60A5FA), fontSize = 12.sp, modifier = Modifier.width(52.dp), fontWeight = FontWeight.Bold)
                    androidx.compose.material3.Slider(
                        value = rgbB,
                        onValueChange = { rgbB = it },
                        valueRange = 0f..255f,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // SELETOR DE CATEGORIA
    if (showCategorySheet) {
        var newCatInput by remember { mutableStateOf("") }

        ModalBottomSheet(
            onDismissRequest = { showCategorySheet = false },
            containerColor = Color(0xFF16181D)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Text("Selecionar ou Criar Categoria", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(14.dp))

                // Criar nova categoria diretamente
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newCatInput,
                        onValueChange = { newCatInput = it },
                        placeholder = { Text("Nova categoria (ex: Estudos)", color = Color.Gray, fontSize = 13.sp) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF2C2F38)
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (newCatInput.isNotBlank()) {
                                val saved = repo.saveCategory(NoteCategory(name = newCatInput.trim()))
                                categories = repo.getCategories()
                                selectedCategoryId = saved.id
                                newCatInput = ""
                                showCategorySheet = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Text("+ Criar")
                    }
                }
                Spacer(modifier = Modifier.height(14.dp))

                // Opção Sem Categoria
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            selectedCategoryId = null
                            showCategorySheet = false
                        },
                    colors = CardDefaults.cardColors(containerColor = if (selectedCategoryId == null) Color(0xFF2563EB) else Color(0xFF1E2026))
                ) {
                    Text("Nenhuma categoria", color = Color.White, modifier = Modifier.padding(14.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))

                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    categories.forEach { cat ->
                        val isSel = selectedCategoryId == cat.id
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    selectedCategoryId = cat.id
                                    showCategorySheet = false
                                },
                            colors = CardDefaults.cardColors(containerColor = if (isSel) Color(0xFF2563EB) else Color(0xFF1E2026))
                        ) {
                            Text(cat.name, color = Color.White, modifier = Modifier.padding(14.dp))
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // DIÁLOGO ADICIONAR ETIQUETA
    if (showTagDialog) {
        AlertDialog(
            onDismissRequest = { showTagDialog = false },
            containerColor = Color(0xFF1E2026),
            title = { Text("Adicionar Etiqueta", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = newTagInput,
                        onValueChange = { newTagInput = it },
                        placeholder = { Text("Nome da etiqueta (ex: urgente)", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF2C2F38)
                        )
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Etiquetas sugeridas:", color = Color.Gray, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        availableTags.forEach { t ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF2C2F38))
                                    .clickable {
                                        if (!tags.contains(t.name)) {
                                            tags = tags + t.name
                                        }
                                        showTagDialog = false
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("#${t.name}", color = Color.LightGray, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = newTagInput.trim().removePrefix("#")
                        if (clean.isNotBlank()) {
                            if (!tags.contains(clean)) {
                                tags = tags + clean
                                repo.saveTag(clean)
                                availableTags = repo.getTags()
                            }
                            newTagInput = ""
                            showTagDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("Adicionar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTagDialog = false }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }
}
