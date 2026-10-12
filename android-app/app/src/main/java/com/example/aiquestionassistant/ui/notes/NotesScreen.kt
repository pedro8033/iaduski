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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.ViewList
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.aiquestionassistant.data.AiRepository
import com.example.aiquestionassistant.data.AppSettings
import com.example.aiquestionassistant.data.Note
import com.example.aiquestionassistant.data.NoteAiAction
import com.example.aiquestionassistant.data.NoteCategory
import com.example.aiquestionassistant.data.NoteColor
import com.example.aiquestionassistant.data.NoteTag
import com.example.aiquestionassistant.data.NoteTemplate
import com.example.aiquestionassistant.data.NoteTemplates
import com.example.aiquestionassistant.data.NotesLocalRepository
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NotesScreen(
    onBack: () -> Unit,
    onOpenNote: (String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repo = remember { NotesLocalRepository(context) }
    val aiRepo = remember { AiRepository() }
    val settings = remember { AppSettings.load(context) }

    // Estados de dados
    var notes by remember { mutableStateOf<List<Note>>(emptyList()) }
    var categories by remember { mutableStateOf<List<NoteCategory>>(emptyList()) }
    var tags by remember { mutableStateOf<List<NoteTag>>(emptyList()) }

    // Estados de controle e navegação
    var searchQuery by remember { mutableStateOf("") }
    var currentTab by remember { mutableStateOf("all") } // all, favorites, pinned, archived, trash
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var selectedTag by remember { mutableStateOf<String?>(null) }
    var selectedColor by remember { mutableStateOf<String?>(null) }
    var isGridView by remember { mutableStateOf(true) }
    var sortBy by remember { mutableStateOf("updated") } // updated, created, title
    var showSortMenu by remember { mutableStateOf(false) }

    // Diálogos e Sheets
    var showTemplatesSheet by remember { mutableStateOf(false) }
    var showManageTaxonomySheet by remember { mutableStateOf(false) }
    var showCreateWithAiDialog by remember { mutableStateOf(false) }
    var aiTopicInput by remember { mutableStateOf("") }
    var isAiGenerating by remember { mutableStateOf(false) }
    var noteToDelete by remember { mutableStateOf<Note?>(null) }

    fun refreshData() {
        notes = repo.getNotes(
            query = searchQuery,
            categoryId = selectedCategoryId,
            tag = selectedTag,
            color = selectedColor,
            status = currentTab,
            sortBy = sortBy
        )
        categories = repo.getCategories()
        tags = repo.getTags()
    }

    LaunchedEffect(searchQuery, currentTab, selectedCategoryId, selectedTag, selectedColor, sortBy) {
        refreshData()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = Color(0xFF171717)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // 1. CABEÇALHO: Título "Notas" + Ações de topo
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Notas",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    // Botão alternar Lista / Grelha
                    IconButton(onClick = { isGridView = !isGridView }) {
                        Icon(
                            if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Alternar Visualização",
                            tint = Color.LightGray
                        )
                    }

                    // Botão Ordenar
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Default.Sort, contentDescription = "Ordenar", tint = Color.LightGray)
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            modifier = Modifier.background(Color(0xFF212121))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Última edição", color = Color.White) },
                                onClick = { sortBy = "updated"; showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Data de criação", color = Color.White) },
                                onClick = { sortBy = "created"; showSortMenu = false }
                            )
                            DropdownMenuItem(
                                text = { Text("Título (A-Z)", color = Color.White) },
                                onClick = { sortBy = "title"; showSortMenu = false }
                            )
                        }
                    }

                    // Botão Gestão de Categorias e Etiquetas
                    IconButton(onClick = { showManageTaxonomySheet = true }) {
                        Icon(Icons.Default.Category, contentDescription = "Categorias e Etiquetas", tint = Color.LightGray)
                    }
                }
            }

            // 2. BOTÕES PRINCIPAIS: "+ Nova nota", "Modelos" e "Criar com IA"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onOpenNote(null) },
                    modifier = Modifier.weight(1.2f).height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("+ Nova nota", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }

                Button(
                    onClick = { showTemplatesSheet = true },
                    modifier = Modifier.weight(1.1f).height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC84B15))
                ) {
                    Icon(Icons.Default.Category, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Modelos", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }

                Button(
                    onClick = { showCreateWithAiDialog = true },
                    modifier = Modifier.weight(1.1f).height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF212124))
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(17.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Criar com IA", color = Color(0xFF38BDF8), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. BARRA DE PESQUISA
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF212121))
                    .border(1.dp, Color(0xFF2F2F2F), RoundedCornerShape(12.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.weight(1f),
                        textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                        singleLine = true,
                        decorationBox = { inner ->
                            if (searchQuery.isEmpty()) {
                                Text("Pesquisar notas pelo título ou conteúdo...", color = Color.Gray, fontSize = 14.sp)
                            }
                            inner()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(20.dp)) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpar", tint = Color.Gray, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. NAVEGAÇÃO DE STATUS: Todas, Favoritas, Fixadas, Arquivadas, Lixo
            val statusTabs = listOf(
                Pair("all", "Todas"),
                Pair("pinned", "Fixadas"),
                Pair("favorites", "Favoritas"),
                Pair("archived", "Arquivadas"),
                Pair("trash", "Lixo")
            )
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(statusTabs) { (key, label) ->
                    val isSelected = currentTab == key
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isSelected) Color(0xFF2563EB) else Color(0xFF212121))
                            .border(1.dp, if (isSelected) Color(0xFF3B82F6) else Color(0xFF2F2F2F), RoundedCornerShape(20.dp))
                            .clickable { currentTab = key }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else Color(0xFFB4B4B4),
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 5. FILTROS RÁPIDOS POR CATEGORIA E COR
            if (categories.isNotEmpty() || selectedCategoryId != null || selectedColor != null || selectedTag != null) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedCategoryId != null || selectedColor != null || selectedTag != null) {
                        item {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color(0xFFEF4444).copy(alpha = 0.2f))
                                    .clickable {
                                        selectedCategoryId = null
                                        selectedColor = null
                                        selectedTag = null
                                    }
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("Limpar filtros ✕", color = Color(0xFFFCA5A5), fontSize = 12.sp)
                            }
                        }
                    }

                    // Categorias
                    items(categories) { cat ->
                        val isCatSelected = selectedCategoryId == cat.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isCatSelected) Color(0xFF0284C7) else Color(0xFF1E2026))
                                .border(1.dp, if (isCatSelected) Color(0xFF38BDF8) else Color(0xFF2C2F38), RoundedCornerShape(16.dp))
                                .clickable {
                                    selectedCategoryId = if (isCatSelected) null else cat.id
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(cat.name, color = if (isCatSelected) Color.White else Color.Gray, fontSize = 12.sp)
                        }
                    }

                    // Cores disponíveis
                    items(NoteColor.entries) { colorEnum ->
                        val isColorSelected = selectedColor.equals(colorEnum.hex, ignoreCase = true) || selectedColor.equals(colorEnum.name, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(colorEnum.hex)))
                                .border(
                                    width = if (isColorSelected) 2.dp else 1.dp,
                                    color = if (isColorSelected) Color.White else Color(0xFF374151),
                                    shape = CircleShape
                                )
                                .clickable {
                                    selectedColor = if (isColorSelected) null else colorEnum.hex
                                }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 6. LISTAGEM OU ESTADO VAZIO
            if (notes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = null,
                            tint = Color(0xFF374151),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty() || selectedCategoryId != null || selectedColor != null)
                                "Nenhuma nota encontrada com os filtros atuais."
                            else
                                "Ainda não tem notas aqui.",
                            color = Color.LightGray,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Toque em '+ Nova nota' ou 'Criar com IA' para começar.",
                            color = Color.Gray,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                if (isGridView) {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 160.dp),
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(notes, key = { it.id }) { note ->
                            NoteCardItem(
                                note = note,
                                categories = categories,
                                onClick = { onOpenNote(note.id) },
                                onTogglePin = {
                                    repo.saveNote(note.copy(isPinned = !note.isPinned))
                                    refreshData()
                                },
                                onToggleFavorite = {
                                    repo.saveNote(note.copy(isFavorite = !note.isFavorite))
                                    refreshData()
                                },
                                onDuplicate = {
                                    repo.duplicateNote(note.id)
                                    refreshData()
                                },
                                onArchive = {
                                    repo.saveNote(note.copy(isArchived = !note.isArchived))
                                    refreshData()
                                },
                                onDelete = { noteToDelete = note },
                                onRestore = {
                                    repo.restoreNote(note.id)
                                    refreshData()
                                }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(notes, key = { it.id }) { note ->
                            NoteCardItem(
                                note = note,
                                categories = categories,
                                onClick = { onOpenNote(note.id) },
                                onTogglePin = {
                                    repo.saveNote(note.copy(isPinned = !note.isPinned))
                                    refreshData()
                                },
                                onToggleFavorite = {
                                    repo.saveNote(note.copy(isFavorite = !note.isFavorite))
                                    refreshData()
                                },
                                onDuplicate = {
                                    repo.duplicateNote(note.id)
                                    refreshData()
                                },
                                onArchive = {
                                    repo.saveNote(note.copy(isArchived = !note.isArchived))
                                    refreshData()
                                },
                                onDelete = { noteToDelete = note },
                                onRestore = {
                                    repo.restoreNote(note.id)
                                    refreshData()
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // MODAL BOTTOM SHEET: MODELOS DE BLOCO DE NOTAS
    if (showTemplatesSheet) {
        val templates = repo.getTemplates()
        ModalBottomSheet(
            onDismissRequest = { showTemplatesSheet = false },
            containerColor = Color(0xFF181A20)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "Modelos de Bloco de Notas",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Escolha um modelo para criar uma nova anotação:",
                    color = Color.LightGray,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(14.dp))

                templates.forEach { tmpl ->
                    val isTerracotta = tmpl.color.equals("#c84b15", ignoreCase = true)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp)
                            .clickable {
                                showTemplatesSheet = false
                                val created = repo.createNoteFromTemplate(tmpl.id)
                                refreshData()
                                onOpenNote(created.id)
                            },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isTerracotta) Color(0xFF261915) else Color(0xFF1E2026)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = if (isTerracotta) Color(0xFFC84B15).copy(alpha = 0.7f) else Color(0xFF2F333D)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isTerracotta) Color(0xFFC84B15) else Color(0xFF2563EB)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isTerracotta) Icons.Default.Category else Icons.Default.Description,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = tmpl.name,
                                        color = Color.White,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (isTerracotta) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFC84B15))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("Novo", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = tmpl.description,
                                    color = Color.LightGray,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // DIÁLOGO: CRIAR COM IA
    if (showCreateWithAiDialog) {
        AlertDialog(
            onDismissRequest = { if (!isAiGenerating) showCreateWithAiDialog = false },
            containerColor = Color(0xFF1E2026),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF38BDF8))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Criar Nota com IA", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text("Digite o assunto, tema ou esboço de notas:", color = Color.LightGray, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = aiTopicInput,
                        onValueChange = { aiTopicInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Ex: Resumo de Fisiologia Cardíaca ou Guia de TypeScript", color = Color.Gray) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF38BDF8),
                            unfocusedBorderColor = Color(0xFF2C2F38)
                        )
                    )
                    if (isAiGenerating) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color(0xFF38BDF8))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("A inteligência artificial está a redigir...", color = Color.LightGray, fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (aiTopicInput.isNotBlank()) {
                            isAiGenerating = true
                            scope.launch {
                                val (ok, result) = aiRepo.executeNoteAction(
                                    settings = settings,
                                    action = NoteAiAction.GENERATE,
                                    topic = aiTopicInput
                                )
                                isAiGenerating = false
                                showCreateWithAiDialog = false
                                if (ok && result.isNotBlank()) {
                                    val newNote = repo.saveNote(
                                        Note(
                                            title = aiTopicInput.take(60),
                                            content = result,
                                            color = "blue"
                                        )
                                    )
                                    aiTopicInput = ""
                                    onOpenNote(newNote.id)
                                } else {
                                    Toast.makeText(context, "Falha ao gerar nota com IA", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    enabled = !isAiGenerating && aiTopicInput.isNotBlank()
                ) {
                    Text("Gerar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCreateWithAiDialog = false },
                    enabled = !isAiGenerating
                ) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }

    // DIÁLOGO: CONFIRMAÇÃO DE EXCLUSÃO
    noteToDelete?.let { note ->
        AlertDialog(
            onDismissRequest = { noteToDelete = null },
            containerColor = Color(0xFF1E2026),
            title = { Text(if (note.isDeleted) "Eliminar definitivamente?" else "Mover para o lixo?", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    if (note.isDeleted)
                        "Esta nota será eliminada permanentemente sem possibilidade de recuperação."
                    else
                        "A nota '${note.title.ifBlank { "Sem título" }}' será movida para o lixo e poderá ser restaurada quando quiser.",
                    color = Color.LightGray,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        repo.deleteNote(note.id, permanent = note.isDeleted)
                        noteToDelete = null
                        refreshData()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626))
                ) {
                    Text("Eliminar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { noteToDelete = null }) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        )
    }

    // BOTTOM SHEET: GESTÃO DE CATEGORIAS E ETIQUETAS
    if (showManageTaxonomySheet) {
        ManageTaxonomySheet(
            categories = categories,
            tags = tags,
            onDismiss = { showManageTaxonomySheet = false },
            onAddCategory = { name ->
                repo.saveCategory(NoteCategory(name = name))
                refreshData()
            },
            onDeleteCategory = { catId ->
                repo.deleteCategory(catId)
                refreshData()
            },
            onAddTag = { name ->
                repo.saveTag(name)
                refreshData()
            },
            onDeleteTag = { tagName ->
                repo.deleteTag(tagName)
                refreshData()
            }
        )
    }
}

// CARD INDIVIDUAL DA NOTA
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NoteCardItem(
    note: Note,
    categories: List<NoteCategory>,
    onClick: () -> Unit,
    onTogglePin: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDuplicate: () -> Unit,
    onArchive: () -> Unit,
    onDelete: () -> Unit,
    onRestore: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val categoryName = categories.find { it.id == note.categoryId }?.name

    val rawColor = note.color.trim()
    val parsedColor = remember(rawColor) {
        try {
            val hex = if (rawColor.startsWith("#")) rawColor else {
                NoteColor.entries.find { it.name.equals(rawColor, ignoreCase = true) }?.hex ?: "#2e80ea"
            }
            Color(android.graphics.Color.parseColor(hex))
        } catch (_: Exception) {
            Color(0xFF2E80EA)
        }
    }

    val isLight = remember(parsedColor) {
        val r = (parsedColor.red * 255).toInt()
        val g = (parsedColor.green * 255).toInt()
        val b = (parsedColor.blue * 255).toInt()
        ((r * 299) + (g * 587) + (b * 114)) / 1000 >= 128
    }

    val cardBg = parsedColor
    val borderCol = if (isLight) Color.Black.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.2f)
    val titleTextColor = if (isLight) Color(0xFF111827) else Color.White
    val bodyTextColor = if (isLight) Color(0xFF374151) else Color(0xFFE5E7EB)
    val tagBgColor = if (isLight) Color.Black.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.15f)
    val footerTextColor = if (isLight) Color(0xFF4B5563) else Color(0xFF9CA3AF)

    val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }
    val dateStr = remember(note.updatedAt) { dateFormat.format(Date(note.updatedAt)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RectangleShape)
            .border(1.dp, borderCol, RectangleShape)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RectangleShape
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Linha Superior: Categoria / Pin / Fav / Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!categoryName.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(tagBgColor)
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(categoryName, color = titleTextColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (note.isPinned) {
                        Icon(Icons.Default.PushPin, contentDescription = "Fixada", tint = Color(0xFFFBBF24), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    if (note.isFavorite) {
                        Icon(Icons.Default.Favorite, contentDescription = "Favorita", tint = Color(0xFFF472B6), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                    }

                    Box {
                        IconButton(onClick = { showMenu = true }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Mais opções", tint = footerTextColor, modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(Color(0xFF212121))
                        ) {
                            if (!note.isDeleted) {
                                DropdownMenuItem(
                                    text = { Text(if (note.isPinned) "Desafixar" else "Fixar no topo", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null, tint = Color.LightGray) },
                                    onClick = { onTogglePin(); showMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (note.isFavorite) "Remover favoritos" else "Adicionar aos favoritos", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.LightGray) },
                                    onClick = { onToggleFavorite(); showMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Duplicar nota", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color.LightGray) },
                                    onClick = { onDuplicate(); showMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text(if (note.isArchived) "Desarquivar" else "Arquivar", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.Archive, contentDescription = null, tint = Color.LightGray) },
                                    onClick = { onArchive(); showMenu = false }
                                )
                            } else {
                                DropdownMenuItem(
                                    text = { Text("Restaurar nota", color = Color.White) },
                                    leadingIcon = { Icon(Icons.Default.Restore, contentDescription = null, tint = Color(0xFF34D399)) },
                                    onClick = { onRestore(); showMenu = false }
                                )
                            }
                            HorizontalDivider(color = Color(0xFF2C2F38))
                            DropdownMenuItem(
                                text = { Text(if (note.isDeleted) "Eliminar permanentemente" else "Mover para lixo", color = Color(0xFFF87171)) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFF87171)) },
                                onClick = { onDelete(); showMenu = false }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Título
            Text(
                text = note.title.ifBlank { "Sem título" },
                color = titleTextColor,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Pré-visualização do Conteúdo
            Text(
                text = note.content.ifBlank { "(Nota vazia)" },
                color = bodyTextColor,
                fontSize = 13.sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            // Tags
            if (note.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    note.tags.take(3).forEach { t ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(tagBgColor)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("#$t", color = titleTextColor, fontSize = 10.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Data
            Text(
                text = dateStr,
                color = footerTextColor,
                fontSize = 11.sp
            )
        }
    }
}

// BOTTOM SHEET DE GESTÃO DE CATEGORIAS E ETIQUETAS
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageTaxonomySheet(
    categories: List<NoteCategory>,
    tags: List<NoteTag>,
    onDismiss: () -> Unit,
    onAddCategory: (String) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onAddTag: (String) -> Unit,
    onDeleteTag: (String) -> Unit
) {
    var newCatName by remember { mutableStateOf("") }
    var newTagName by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF181A20)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text("Gestão de Categorias e Etiquetas", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            // Secção Categorias
            Text("Categorias", color = Color(0xFF38BDF8), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newCatName,
                    onValueChange = { newCatName = it },
                    placeholder = { Text("Nova categoria (ex: Estudos)", color = Color.Gray, fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
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
                        if (newCatName.isNotBlank()) {
                            onAddCategory(newCatName.trim())
                            newCatName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                ) {
                    Text("+")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF262933))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(cat.name, color = Color.White, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Eliminar",
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp).clickable { onDeleteCategory(cat.id) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Secção Etiquetas
            Text("Etiquetas", color = Color(0xFF34D399), fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newTagName,
                    onValueChange = { newTagName = it },
                    placeholder = { Text("Nova etiqueta (ex: importante)", color = Color.Gray, fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF34D399),
                        unfocusedBorderColor = Color(0xFF2C2F38)
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (newTagName.isNotBlank()) {
                            onAddTag(newTagName.trim().removePrefix("#"))
                            newTagName = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669))
                ) {
                    Text("+")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tags) { tag ->
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF262933))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("#${tag.name}", color = Color.White, fontSize = 12.sp)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Eliminar",
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp).clickable { onDeleteTag(tag.name) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
