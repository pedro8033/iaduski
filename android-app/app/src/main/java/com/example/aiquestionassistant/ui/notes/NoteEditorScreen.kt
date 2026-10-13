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
import androidx.compose.material.icons.filled.Category
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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.filled.BorderColor
import androidx.compose.material.icons.filled.FormatColorText
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.TextIncrease
import androidx.compose.material.icons.filled.TextDecrease
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
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
import com.example.aiquestionassistant.data.NoteTemplates
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
    var showTextColorSheet by remember { mutableStateOf(false) }
    var showTextSizeSheet by remember { mutableStateOf(false) }
    var editorFontSizeSp by remember { mutableStateOf(16f) }
    var showCategorySheet by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }
    var newTagInput by remember { mutableStateOf("") }

    // Estado de Pré-visualização da IA (Requisito 5: NUNCA substituir silenciosamente)
    var aiPreviewResult by remember { mutableStateOf<Pair<NoteAiAction, String>?>(null) }
    var isAiProcessing by remember { mutableStateOf(false) }
    var isPreviewMode by remember { mutableStateOf(false) }

    // Launcher para Tirar Foto com a Câmera diretamente
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            try {
                val mediaDir = File(context.filesDir, "notes_media").apply { mkdirs() }
                val imageFile = File(mediaDir, "cam_${System.currentTimeMillis()}.jpg")
                imageFile.outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                }
                val path = imageFile.absolutePath
                val insertText = if (contentValue.text.isBlank() || contentValue.text.endsWith("\n\n")) {
                    "![Foto da Câmera]($path)\n\n"
                } else if (contentValue.text.endsWith("\n")) {
                    "\n![Foto da Câmera]($path)\n\n"
                } else {
                    "\n\n![Foto da Câmera]($path)\n\n"
                }
                val sel = contentValue.selection
                val original = contentValue.text
                val start = sel.start.coerceIn(0, original.length)
                val end = sel.end.coerceIn(0, original.length)
                val before = original.substring(0, start)
                val after = original.substring(end)
                val newText = before + insertText + after
                val newCursor = start + insertText.length
                contentValue = TextFieldValue(newText, TextRange(newCursor))
                Toast.makeText(context, "Foto da câmera inserida na nota!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Erro ao processar foto: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Launcher para Inserir Foto da Galeria / Ficheiros
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val mediaDir = File(context.filesDir, "notes_media").apply { mkdirs() }
                val imageFile = File(mediaDir, "gallery_${System.currentTimeMillis()}.jpg")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    imageFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                val path = imageFile.absolutePath
                val insertText = if (contentValue.text.isBlank() || contentValue.text.endsWith("\n\n")) {
                    "![Foto]($path)\n\n"
                } else if (contentValue.text.endsWith("\n")) {
                    "\n![Foto]($path)\n\n"
                } else {
                    "\n\n![Foto]($path)\n\n"
                }
                val sel = contentValue.selection
                val original = contentValue.text
                val start = sel.start.coerceIn(0, original.length)
                val end = sel.end.coerceIn(0, original.length)
                val before = original.substring(0, start)
                val after = original.substring(end)
                val newText = before + insertText + after
                val newCursor = start + insertText.length
                contentValue = TextFieldValue(newText, TextRange(newCursor))
                Toast.makeText(context, "Foto adicionada à nota!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(context, "Erro ao carregar foto: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

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

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // Botão Alternar Visualização / Edição
                    Button(
                        onClick = { isPreviewMode = !isPreviewMode },
                        colors = ButtonDefaults.buttonColors(containerColor = if (isPreviewMode) Color(0xFFC84B15) else Color(0xFF262933)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(if (isPreviewMode) "Editar" else "Ver", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

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

            // BARRA DE FORMATAÇÃO RICA RÁPIDA (Negrito, Itálico, Destacar, Cor, Tamanho, Listas, Tabelas)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF141416))
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 8.dp, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Negrito
                IconButton(
                    onClick = {
                        contentValue = applyWrapOrInsert(contentValue, "**", "**", "texto em negrito")
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Default.FormatBold, contentDescription = "Negrito", tint = Color.White, modifier = Modifier.size(19.dp))
                }

                // 2. Itálico
                IconButton(
                    onClick = {
                        contentValue = applyWrapOrInsert(contentValue, "*", "*", "texto em itálico")
                    },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(Icons.Default.FormatItalic, contentDescription = "Itálico", tint = Color.LightGray, modifier = Modifier.size(19.dp))
                }

                // 3. Destacar Texto (Highlight)
                IconButton(
                    onClick = {
                        contentValue = applyWrapOrInsert(contentValue, "==", "==", "texto destacado")
                    },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFEF08A).copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Default.BorderColor, contentDescription = "Destacar Texto", tint = Color(0xFFFDE047), modifier = Modifier.size(18.dp))
                }

                // 4. Mudar de Cor do Texto
                IconButton(
                    onClick = { showTextColorSheet = true },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                ) {
                    Icon(Icons.Default.FormatColorText, contentDescription = "Mudar Cor do Texto", tint = Color(0xFFF87171), modifier = Modifier.size(19.dp))
                }

                // 5. Aumentar Tamanho / Título (Opções H1, H2, H3, Grande)
                IconButton(
                    onClick = { showTextSizeSheet = true },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF38BDF8).copy(alpha = 0.15f))
                ) {
                    Icon(Icons.Default.FormatSize, contentDescription = "Tamanho do Texto", tint = Color(0xFF38BDF8), modifier = Modifier.size(19.dp))
                }

                // Atalhos rápidos de zoom da fonte do editor (A- / A+)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF22252F))
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "A-",
                        color = Color.LightGray,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                editorFontSizeSp = (editorFontSizeSp - 2f).coerceAtLeast(12f)
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                    Text(
                        text = "${editorFontSizeSp.toInt()}",
                        color = Color(0xFF38BDF8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "A+",
                        color = Color.LightGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable {
                                editorFontSizeSp = (editorFontSizeSp + 2f).coerceAtMost(32f)
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                // Tirar Foto diretamente com a Câmera
                IconButton(
                    onClick = { cameraLauncher.launch(null) },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF0284C7).copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Tirar Foto (Câmera)", tint = Color(0xFF38BDF8), modifier = Modifier.size(19.dp))
                }

                // Inserir Foto da Galeria
                IconButton(
                    onClick = { galleryLauncher.launch("image/*") },
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF8B5CF6).copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Default.Image, contentDescription = "Inserir Foto (Galeria)", tint = Color(0xFFA78BFA), modifier = Modifier.size(19.dp))
                }

                // 6. Lista com marcadores
                IconButton(onClick = {
                    val sel = contentValue.selection
                    val text = contentValue.text
                    val before = text.substring(0, sel.start)
                    val after = text.substring(sel.start)
                    val newText = if (before.endsWith("\n") || before.isEmpty()) "$before- $after" else "$before\n- $after"
                    contentValue = TextFieldValue(newText, TextRange(newText.length - after.length))
                }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.FormatListBulleted, contentDescription = "Lista", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                }

                // 7. Lista de Tarefas (Checklist)
                IconButton(onClick = {
                    val sel = contentValue.selection
                    val text = contentValue.text
                    val before = text.substring(0, sel.start)
                    val after = text.substring(sel.start)
                    val newText = if (before.endsWith("\n") || before.isEmpty()) "$before- [ ] $after" else "$before\n- [ ] $after"
                    contentValue = TextFieldValue(newText, TextRange(newText.length - after.length))
                }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Checklist, contentDescription = "Checklist", tint = Color.LightGray, modifier = Modifier.size(18.dp))
                }

                // 8. Inserir Modelo de Tabela PowerPoint (2 Colunas)
                IconButton(onClick = {
                    val tmpl = NoteTemplates.POWERPOINT_TABLE
                    if (contentValue.text.isBlank()) {
                        contentValue = TextFieldValue(tmpl.content)
                        if (title.isBlank()) title = tmpl.title
                        customColorHex = tmpl.color
                    } else {
                        contentValue = TextFieldValue("${contentValue.text}\n\n${tmpl.content}")
                    }
                    Toast.makeText(context, "Modelo de tabela adicionado!", Toast.LENGTH_SHORT).show()
                }, modifier = Modifier.size(34.dp)) {
                    Icon(Icons.Default.Category, contentDescription = "Inserir Tabela Modelo PowerPoint", tint = Color(0xFFC84B15), modifier = Modifier.size(18.dp))
                }

                // 9. Categoria e Tags seletores
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF2563EB).copy(alpha = 0.25f))
                        .clickable { showCategorySheet = true }
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    val catName = categories.find { it.id == selectedCategoryId }?.name ?: "Sem Categoria"
                    Text(catName, color = Color(0xFF93C5FD), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                }

                IconButton(onClick = { showTagDialog = true }, modifier = Modifier.size(34.dp)) {
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

                // CONTEÚDO DA NOTA (Edição ou Pré-visualização rica do modelo)
                if (isPreviewMode) {
                    NoteTablePreview(
                        content = contentValue.text,
                        editorSubTextColor = editorSubTextColor
                    )
                } else {
                    BasicTextField(
                        value = contentValue,
                        onValueChange = { contentValue = it },
                        modifier = Modifier.fillMaxWidth().height(450.dp),
                        visualTransformation = remember(editorSubTextColor, editorFontSizeSp) {
                            NoteRichTextVisualTransformation(editorSubTextColor, editorFontSizeSp)
                        },
                        textStyle = TextStyle(
                            color = editorSubTextColor,
                            fontSize = editorFontSizeSp.sp,
                            lineHeight = (editorFontSizeSp * 1.45f).sp
                        ),
                        decorationBox = { inner ->
                            if (contentValue.text.isEmpty()) {
                                Text("Escreva a sua nota aqui...", color = editorSubTextColor.copy(alpha = 0.6f), fontSize = editorFontSizeSp.sp)
                            }
                            inner()
                        }
                    )
                }
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

    // MODAL BOTTOM SHEET: COR DO TEXTO (REQUISITO: MUDAR COR DO TEXTO)
    if (showTextColorSheet) {
        val colorsList = listOf(
            Pair("#EF4444", "Vermelho"),
            Pair("#F97316", "Laranja"),
            Pair("#FBBF24", "Amarelo"),
            Pair("#22C55E", "Verde"),
            Pair("#06B6D4", "Ciano"),
            Pair("#38BDF8", "Azul Céu"),
            Pair("#2563EB", "Azul Real"),
            Pair("#A855F7", "Roxo"),
            Pair("#F472B6", "Rosa"),
            Pair("#FFFFFF", "Branco"),
            Pair("#9CA3AF", "Cinza"),
            Pair("#111827", "Escuro")
        )

        ModalBottomSheet(
            onDismissRequest = { showTextColorSheet = false },
            containerColor = Color(0xFF181A20)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FormatColorText, contentDescription = null, tint = Color(0xFFF87171))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mudar Cor do Texto", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                val selText = if (contentValue.selection.start != contentValue.selection.end) {
                    val start = minOf(contentValue.selection.start, contentValue.selection.end)
                    val end = maxOf(contentValue.selection.start, contentValue.selection.end)
                    contentValue.text.substring(start, end)
                } else ""
                Text(
                    text = if (selText.isNotBlank()) "Aplicar sobre o trecho: \"${selText.take(25)}\"" else "A cor será aplicada ao novo texto",
                    color = Color.LightGray,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    colorsList.forEach { (hex, label) ->
                        val col = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.White }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable {
                                    contentValue = applyWrapOrInsert(contentValue, "<color=$hex>", "</color>", "texto colorido")
                                    showTextColorSheet = false
                                }
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(col)
                                    .border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("A", color = if (hex == "#FFFFFF" || hex == "#FBBF24") Color.Black else Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(label, color = Color.LightGray, fontSize = 10.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // MODAL BOTTOM SHEET: TAMANHO DO TEXTO & TÍTULOS (REQUISITO: AUMENTAR TAMANHO)
    if (showTextSizeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTextSizeSheet = false },
            containerColor = Color(0xFF181A20)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.FormatSize, contentDescription = null, tint = Color(0xFF38BDF8))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Aumentar Tamanho do Texto", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(14.dp))

                val sizeOptions = listOf(
                    Triple("Título Principal (H1)", "# ", 24),
                    Triple("Subtítulo (H2)", "## ", 20),
                    Triple("Seção (H3)", "### ", 17),
                    Triple("Trecho em Destaque Grande", "[size:large]", 19),
                    Triple("Trecho Enorme", "[size:huge]", 23)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sizeOptions.forEach { (label, prefix, fontSizeVal) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (prefix.startsWith("#")) {
                                        val sel = contentValue.selection
                                        val text = contentValue.text
                                        val before = text.substring(0, sel.start)
                                        val after = text.substring(sel.start)
                                        val newText = if (before.endsWith("\n") || before.isEmpty()) "$before$prefix$after" else "$before\n$prefix$after"
                                        contentValue = TextFieldValue(newText, TextRange(newText.length - after.length))
                                    } else {
                                        val tag = prefix.removePrefix("[").removeSuffix("]")
                                        contentValue = applyWrapOrInsert(contentValue, "[$tag]", "[/${tag.substringBefore(":")}]", "texto grande")
                                    }
                                    showTextSizeSheet = false
                                },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF22252F)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Aa",
                                    color = Color(0xFF38BDF8),
                                    fontSize = (fontSizeVal * 0.9f).sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.width(44.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(label, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Estilo $prefix", color = Color.Gray, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = Color(0xFF2E323D))
                Spacer(modifier = Modifier.height(12.dp))

                // Zoom geral da fonte do bloco
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Tamanho Geral do Bloco:", color = Color.LightGray, fontSize = 13.sp)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { editorFontSizeSp = (editorFontSizeSp - 2f).coerceAtLeast(12f) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D313D)),
                            modifier = Modifier.size(36.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Text("-", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("${editorFontSizeSp.toInt()} sp", color = Color(0xFF38BDF8), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp))
                        Button(
                            onClick = { editorFontSizeSp = (editorFontSizeSp + 2f).coerceAtMost(32f) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                            modifier = Modifier.size(36.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
                        ) {
                            Text("+", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// UTILITÁRIO DE ENVOLVER OU INSERIR FORMATAÇÃO
fun applyWrapOrInsert(
    current: TextFieldValue,
    prefix: String,
    suffix: String,
    placeholder: String
): TextFieldValue {
    val sel = current.selection
    val text = current.text
    val start = minOf(sel.start, sel.end)
    val end = maxOf(sel.start, sel.end)

    val before = text.substring(0, start)
    val after = text.substring(end)

    return if (start != end) {
        val selected = text.substring(start, end)
        if (selected.startsWith(prefix) && selected.endsWith(suffix) && selected.length >= prefix.length + suffix.length) {
            val unwrapped = selected.substring(prefix.length, selected.length - suffix.length)
            val newText = "$before$unwrapped$after"
            TextFieldValue(newText, TextRange(start, start + unwrapped.length))
        } else {
            val newText = "$before$prefix$selected$suffix$after"
            TextFieldValue(newText, TextRange(start + prefix.length, end + prefix.length))
        }
    } else {
        val newText = "$before$prefix$placeholder$suffix$after"
        TextFieldValue(newText, TextRange(start + prefix.length, start + prefix.length + placeholder.length))
    }
}

// VISUAL TRANSFORMATION PARA O EDITOR DE TEXTO RICO (NEGRITO, DESTAQUE, CORES E TAMANHOS EM TEMPO REAL)
class NoteRichTextVisualTransformation(
    private val defaultColor: Color,
    private val baseFontSize: Float = 16f
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val builder = AnnotatedString.Builder(raw)

        // 1. Negrito: **...**
        val boldRegex = Regex("""\*\*(.+?)\*\*""")
        boldRegex.findAll(raw).forEach { match ->
            val range = match.groups[1]!!.range
            builder.addStyle(
                SpanStyle(fontWeight = FontWeight.Bold, color = defaultColor),
                range.first,
                range.last + 1
            )
            builder.addStyle(
                SpanStyle(color = defaultColor.copy(alpha = 0.35f)),
                match.range.first,
                match.range.first + 2
            )
            builder.addStyle(
                SpanStyle(color = defaultColor.copy(alpha = 0.35f)),
                match.range.last - 1,
                match.range.last + 1
            )
        }

        // 2. Itálico: *...*
        val italicRegex = Regex("""(?<!\*)\*([^*]+?)\*(?!\*)""")
        italicRegex.findAll(raw).forEach { match ->
            val range = match.groups[1]!!.range
            builder.addStyle(
                SpanStyle(fontStyle = FontStyle.Italic),
                range.first,
                range.last + 1
            )
            builder.addStyle(
                SpanStyle(color = defaultColor.copy(alpha = 0.35f)),
                match.range.first,
                match.range.first + 1
            )
            builder.addStyle(
                SpanStyle(color = defaultColor.copy(alpha = 0.35f)),
                match.range.last,
                match.range.last + 1
            )
        }

        // 3. Destacar: ==...== ou <mark>...</mark>
        val highlightRegex = Regex("""(==|<mark>)(.+?)(==|</mark>)""")
        highlightRegex.findAll(raw).forEach { match ->
            val openTag = match.groups[1]!!.value
            val closeTag = match.groups[3]!!.value
            val contentGroup = match.groups[2]!!

            builder.addStyle(
                SpanStyle(
                    background = Color(0xFFFEF08A),
                    color = Color(0xFF1E293B),
                    fontWeight = FontWeight.SemiBold
                ),
                contentGroup.range.first,
                contentGroup.range.last + 1
            )
            builder.addStyle(
                SpanStyle(color = Color(0xFFCA8A04).copy(alpha = 0.5f), fontSize = (baseFontSize * 0.8f).sp),
                match.range.first,
                match.range.first + openTag.length
            )
            builder.addStyle(
                SpanStyle(color = Color(0xFFCA8A04).copy(alpha = 0.5f), fontSize = (baseFontSize * 0.8f).sp),
                match.range.last - closeTag.length + 1,
                match.range.last + 1
            )
        }

        // 4. Mudar de Cor: <color=#hex>...</color> ou [color:#hex]...[/color]
        val colorRegex = Regex("""(<color=(#[0-9a-fA-F]{6}|#[0-9a-fA-F]{8})>|\[color:(#[0-9a-fA-F]{6}|#[0-9a-fA-F]{8})\])(.+?)(</color>|\[/color\])""")
        colorRegex.findAll(raw).forEach { match ->
            val hex = match.groups[2]?.value ?: match.groups[3]?.value ?: ""
            val contentGroup = match.groups[4]!!
            try {
                val parsedColor = Color(android.graphics.Color.parseColor(hex))
                builder.addStyle(
                    SpanStyle(color = parsedColor, fontWeight = FontWeight.Bold),
                    contentGroup.range.first,
                    contentGroup.range.last + 1
                )
            } catch (_: Exception) {}
            val openTag = match.groups[1]!!.value
            val closeTag = match.groups[5]!!.value
            builder.addStyle(
                SpanStyle(color = defaultColor.copy(alpha = 0.35f), fontSize = (baseFontSize * 0.75f).sp),
                match.range.first,
                match.range.first + openTag.length
            )
            builder.addStyle(
                SpanStyle(color = defaultColor.copy(alpha = 0.35f), fontSize = (baseFontSize * 0.75f).sp),
                match.range.last - closeTag.length + 1,
                match.range.last + 1
            )
        }

        // 5. Aumentar Tamanho: [size:large]...[/size] ou [size:huge]...[/size]
        val sizeRegex = Regex("""\[size:(large|huge)\](.+?)\[/size\]""")
        sizeRegex.findAll(raw).forEach { match ->
            val sizeType = match.groups[1]!!.value
            val contentGroup = match.groups[2]!!
            val targetSp = if (sizeType == "huge") (baseFontSize * 1.55f).sp else (baseFontSize * 1.3f).sp
            builder.addStyle(
                SpanStyle(fontSize = targetSp, fontWeight = FontWeight.Bold),
                contentGroup.range.first,
                contentGroup.range.last + 1
            )
        }

        // 6. Linhas de Título (# , ## , ###)
        val lines = raw.split("\n")
        var currentOffset = 0
        for (line in lines) {
            val lineLen = line.length
            if (line.startsWith("# ")) {
                builder.addStyle(
                    SpanStyle(fontSize = (baseFontSize * 1.5f).sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8)),
                    currentOffset,
                    currentOffset + lineLen
                )
            } else if (line.startsWith("## ")) {
                builder.addStyle(
                    SpanStyle(fontSize = (baseFontSize * 1.3f).sp, fontWeight = FontWeight.Bold, color = Color(0xFF60A5FA)),
                    currentOffset,
                    currentOffset + lineLen
                )
            } else if (line.startsWith("### ")) {
                builder.addStyle(
                    SpanStyle(fontSize = (baseFontSize * 1.15f).sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF93C5FD)),
                    currentOffset,
                    currentOffset + lineLen
                )
            }
            currentOffset += lineLen + 1
        }

        // 7. Imagens Markdown: ![Alt](caminho)
        val imgRegex = Regex("""!\[(.*?)\]\((.*?)\)""")
        imgRegex.findAll(raw).forEach { match ->
            builder.addStyle(
                SpanStyle(color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold),
                match.range.first,
                match.range.last + 1
            )
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}

// PARSER DE MARKDOWN RICO PARA MODO DE PRÉ-VISUALIZAÇÃO (SEM MARCADORES BRUTOS)
fun parseRichMarkdownToAnnotatedString(
    line: String,
    defaultColor: Color,
    baseFontSize: Float = 15f
): AnnotatedString {
    val builder = AnnotatedString.Builder()

    // Regex para identificar e retirar as tags, aplicando estilos sobre o texto limpo
    val tokenRegex = Regex("""(\*\*(.+?)\*\*)|(==(.+?)==|<mark>(.+?)</mark>)|(<color=(#[0-9a-fA-F]{6}|#[0-9a-fA-F]{8})>(.+?)</color>|\[color:(#[0-9a-fA-F]{6}|#[0-9a-fA-F]{8})\](.+?)\[/color\])|(\[size:(large|huge)\](.+?)\[/size\])|(?<!\*)\*([^*]+?)\*(?!\*)""")

    var lastIdx = 0
    tokenRegex.findAll(line).forEach { match ->
        if (match.range.first > lastIdx) {
            val normalText = line.substring(lastIdx, match.range.first)
            builder.append(normalText)
        }

        val fullMatch = match.value
        when {
            // Negrito
            fullMatch.startsWith("**") && fullMatch.endsWith("**") -> {
                val content = fullMatch.removeSurrounding("**")
                val start = builder.length
                builder.append(content)
                builder.addStyle(SpanStyle(fontWeight = FontWeight.Bold, color = defaultColor), start, builder.length)
            }
            // Destacar
            (fullMatch.startsWith("==") && fullMatch.endsWith("==")) || (fullMatch.startsWith("<mark>") && fullMatch.endsWith("</mark>")) -> {
                val content = if (fullMatch.startsWith("==")) fullMatch.removeSurrounding("==") else fullMatch.removePrefix("<mark>").removeSuffix("</mark>")
                val start = builder.length
                builder.append(content)
                builder.addStyle(
                    SpanStyle(background = Color(0xFFFEF08A), color = Color(0xFF1E293B), fontWeight = FontWeight.SemiBold),
                    start, builder.length
                )
            }
            // Cor
            fullMatch.startsWith("<color=") || fullMatch.startsWith("[color:") -> {
                val hex = match.groups[7]?.value ?: match.groups[9]?.value ?: "#FFFFFF"
                val content = match.groups[8]?.value ?: match.groups[10]?.value ?: ""
                val start = builder.length
                builder.append(content)
                try {
                    val parsedColor = Color(android.graphics.Color.parseColor(hex))
                    builder.addStyle(SpanStyle(color = parsedColor, fontWeight = FontWeight.Bold), start, builder.length)
                } catch (_: Exception) {}
            }
            // Tamanho
            fullMatch.startsWith("[size:") -> {
                val sizeType = match.groups[12]?.value ?: "large"
                val content = match.groups[13]?.value ?: ""
                val targetSp = if (sizeType == "huge") (baseFontSize * 1.55f).sp else (baseFontSize * 1.3f).sp
                val start = builder.length
                builder.append(content)
                builder.addStyle(SpanStyle(fontSize = targetSp, fontWeight = FontWeight.Bold), start, builder.length)
            }
            // Itálico
            fullMatch.startsWith("*") && fullMatch.endsWith("*") -> {
                val content = fullMatch.removeSurrounding("*")
                val start = builder.length
                builder.append(content)
                builder.addStyle(SpanStyle(fontStyle = FontStyle.Italic, color = defaultColor), start, builder.length)
            }
            else -> {
                builder.append(fullMatch)
            }
        }
        lastIdx = match.range.last + 1
    }

    if (lastIdx < line.length) {
        builder.append(line.substring(lastIdx))
    }

    return builder.toAnnotatedString()
}

@Composable
fun NoteTablePreview(
    content: String,
    editorSubTextColor: Color
) {
    val lines = content.lines()
    val tableLines = mutableListOf<String>()
    var inTable = false

    Column(modifier = Modifier.fillMaxWidth()) {
        lines.forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("|") && trimmed.endsWith("|")) {
                tableLines.add(trimmed)
                inTable = true
            } else {
                if (inTable && tableLines.isNotEmpty()) {
                    RenderMarkdownTable(tableLines)
                    tableLines.clear()
                    inTable = false
                }
                if (trimmed.isNotBlank()) {
                    when {
                        // Título H1
                        trimmed.startsWith("# ") -> {
                            val cleanTitle = trimmed.removePrefix("# ").trim()
                            Text(
                                text = cleanTitle,
                                color = Color(0xFF38BDF8),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                            )
                        }
                        // Subtítulo H2
                        trimmed.startsWith("## ") -> {
                            val cleanTitle = trimmed.removePrefix("## ").trim()
                            Text(
                                text = cleanTitle,
                                color = Color(0xFF60A5FA),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        // Seção H3
                        trimmed.startsWith("### ") -> {
                            val cleanTitle = trimmed.removePrefix("### ").trim()
                            Text(
                                text = cleanTitle,
                                color = Color(0xFF93C5FD),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                            )
                        }
                        // Checklist marcado (- [x] ou - [X])
                        trimmed.startsWith("- [x] ", ignoreCase = true) -> {
                            val taskText = trimmed.substring(6).trim()
                            Row(
                                modifier = Modifier.padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckBox, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = parseRichMarkdownToAnnotatedString(taskText, editorSubTextColor.copy(alpha = 0.7f)),
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                        // Checklist desmarcado (- [ ])
                        trimmed.startsWith("- [ ] ") -> {
                            val taskText = trimmed.substring(6).trim()
                            Row(
                                modifier = Modifier.padding(vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckBoxOutlineBlank, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = parseRichMarkdownToAnnotatedString(taskText, editorSubTextColor),
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                        // Marcador de lista (- )
                        trimmed.startsWith("- ") -> {
                            val itemText = trimmed.substring(2).trim()
                            Row(
                                modifier = Modifier.padding(vertical = 3.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("• ", color = Color(0xFF38BDF8), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = parseRichMarkdownToAnnotatedString(itemText, editorSubTextColor),
                                    fontSize = 15.sp,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                        // Imagem Markdown: ![Alt](caminho)
                        trimmed.startsWith("![") && trimmed.contains("](") && trimmed.endsWith(")") -> {
                            val alt = trimmed.substringAfter("![").substringBefore("]")
                            val imgPath = trimmed.substringAfter("](").substringBeforeLast(")")
                            RenderNoteImage(path = imgPath, alt = alt)
                        }
                        // Parágrafo com formatação rica
                        else -> {
                            Text(
                                text = parseRichMarkdownToAnnotatedString(line, editorSubTextColor),
                                fontSize = 15.sp,
                                lineHeight = 22.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
        if (tableLines.isNotEmpty()) {
            RenderMarkdownTable(tableLines)
        }
    }
}

@Composable
fun RenderNoteImage(path: String, alt: String) {
    val context = LocalContext.current
    var bitmap by remember(path) { mutableStateOf<Bitmap?>(null) }
    var loadFailed by remember(path) { mutableStateOf(false) }

    LaunchedEffect(path) {
        withContext(Dispatchers.IO) {
            try {
                if (path.startsWith("data:image")) {
                    val base64 = path.substringAfter("base64,")
                    val bytes = Base64.decode(base64, Base64.DEFAULT)
                    bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                } else if (path.startsWith("content://") || path.startsWith("file://")) {
                    val uri = Uri.parse(path)
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        bitmap = BitmapFactory.decodeStream(stream)
                    }
                } else {
                    val file = File(path)
                    if (file.exists()) {
                        bitmap = BitmapFactory.decodeFile(file.absolutePath)
                    } else {
                        loadFailed = true
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                loadFailed = true
            }
        }
    }

    if (bitmap != null) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = alt,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                )
            }
            if (alt.isNotBlank() && alt != "Foto" && alt != "Imagem" && alt != "Foto da Câmera") {
                Text(
                    text = alt,
                    color = Color.LightGray,
                    fontSize = 12.sp,
                    fontStyle = FontStyle.Italic,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
            }
        }
    } else {
        Card(
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF181B26)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (loadFailed) "⚠️ Imagem não encontrada: $alt" else "📸 $alt (${File(path).name})",
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun RenderMarkdownTable(lines: List<String>) {
    if (lines.isEmpty()) return
    val headerCells = lines[0].split("|").map { it.trim() }.filter { it.isNotEmpty() }
    val dataRows = lines.drop(1).filter { line ->
        val clean = line.replace("|", "").replace("-", "").replace(":", "").trim()
        clean.isNotEmpty()
    }.map { line ->
        line.split("|").map { it.trim() }.filter { it.isNotEmpty() }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF7D7D2))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFC84B15))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                headerCells.forEachIndexed { idx, titleText ->
                    Text(
                        text = titleText,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        modifier = Modifier.weight(if (idx == 0) 1f else 1.8f)
                    )
                }
            }

            dataRows.forEachIndexed { rowIdx, rowCells ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(if (rowIdx % 2 == 0) Color(0xFFF7D7D2) else Color(0xFFFDE8E5))
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    rowCells.forEachIndexed { idx, cellText ->
                        val cleanCell = cellText.removePrefix("**").removeSuffix("**").trim()
                        Text(
                            text = cleanCell,
                            color = if (idx == 0) Color(0xFF991B1B) else Color(0xFF1F2937),
                            fontWeight = if (idx == 0) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 14.sp,
                            modifier = Modifier.weight(if (idx == 0) 1f else 1.8f)
                        )
                    }
                }
                if (rowIdx < dataRows.size - 1) {
                    HorizontalDivider(color = Color(0xFFE8B4AC), thickness = 0.8.dp)
                }
            }
        }
    }
}


