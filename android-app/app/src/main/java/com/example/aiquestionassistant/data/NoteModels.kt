package com.example.aiquestionassistant.data

import kotlinx.serialization.Serializable

@Serializable
enum class NoteColor(val hex: String, val label: String) {
    BLUE("#2e80ea", "Azul"),
    LEMON("#eeff00", "Amarelo-limão"),
    GREEN("#82ff80", "Verde-claro"),
    GOLD("#ffd91a", "Amarelo-ouro"),
    GRAY("#bdbdbd", "Cinzento"),
    TERRACOTTA("#c84b15", "Terracota");

    companion object {
        fun fromString(name: String?): NoteColor {
            if (name == null) return BLUE
            return try {
                valueOf(name.uppercase())
            } catch (e: Exception) {
                // Se for um hex customizado ou não encontrado
                BLUE
            }
        }

        fun parseColor(colorStr: String?): android.graphics.Color? {
            if (colorStr.isNullOrBlank()) return null
            return try {
                val hex = if (colorStr.startsWith("#")) colorStr else {
                    try {
                        valueOf(colorStr.uppercase()).hex
                    } catch (_: Exception) {
                        "#2e80ea"
                    }
                }
                android.graphics.Color.valueOf(android.graphics.Color.parseColor(hex))
            } catch (_: Exception) {
                null
            }
        }
    }
}

@Serializable
data class NoteCategory(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val color: String? = null,
    val icon: String? = null,
    val description: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class NoteTag(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val color: String? = null
)

@Serializable
data class Note(
    val id: String = java.util.UUID.randomUUID().toString(),
    val userId: String? = null,
    val title: String = "",
    val content: String = "",
    val color: String = "default",
    val categoryId: String? = null,
    val tags: List<String> = emptyList(),
    val isPinned: Boolean = false,
    val isFavorite: Boolean = false,
    val isArchived: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

enum class NoteAiAction(val id: String, val label: String, val description: String) {
    GENERATE("generate", "Gerar nota", "Criar nota a partir de um tema"),
    SUMMARIZE("summarize", "Resumir", "Transformar texto extenso num resumo"),
    ORGANIZE("organize", "Organizar", "Estruturar notas em secções"),
    IMPROVE("improve", "Melhorar texto", "Corrigir gramática e clareza"),
    EXPAND("expand", "Expandir conteúdo", "Desenvolver ideia ou explicação"),
    KEYPOINTS("keypoints", "Extrair pontos-chave", "Identificar conceitos importantes"),
    CHECKLIST("checklist", "Criar checklist", "Transformar conteúdo em tarefas"),
    REVIEW_QUESTIONS("review_questions", "Perguntas de revisão", "Gerar perguntas e respostas"),
    SUGGEST_TITLE("suggest_title", "Sugerir título", "Propor título adequado"),
    SUGGEST_TAXONOMY("suggest_taxonomy", "Sugerir categorias/tags", "Recomendar organização relevante"),
    ILLUSTRATION("illustration", "Gerar ilustração", "Criar arte ou imagem com IA")
}

@Serializable
data class NoteTemplate(
    val id: String,
    val name: String,
    val description: String,
    val title: String,
    val content: String,
    val color: String = "#2e80ea",
    val tags: List<String> = emptyList()
)

object NoteTemplates {
    // Modelo atual (em branco/livre) mantido intacto
    val BLANK = NoteTemplate(
        id = "default_blank",
        name = "Bloco Padrão (Livre)",
        description = "Modelo clássico de anotações livres em branco com suporte a Markdown",
        title = "",
        content = "",
        color = "#2e80ea",
        tags = emptyList()
    )

    // Modelo de Terminologia Cirúrgica / Médica (Segundo elemento e Significado) mantido e limpo
    val SURGICAL_TERMINOLOGY = NoteTemplate(
        id = "surgical_terminology_table",
        name = "Tabela: Segundo elemento & Significado",
        description = "Modelo de terminologia cirúrgica e médica com prefixos/sufixos e significados",
        title = "Segundo Elemento e Significado",
        content = """
| Segundo elemento | Significado |
| :--- | :--- |
| | |
| | |
| | |
| | |
| | |
        """.trimIndent(),
        color = "#c84b15",
        tags = listOf("terminologia", "cirurgia", "sufixos")
    )

    // Novo modelo PowerPoint 2 colunas anexado pelo usuário (13 linhas e cabeçalho terracota)
    val POWERPOINT_TABLE = NoteTemplate(
        id = "powerpoint_table",
        name = "Tabela PowerPoint (2 Colunas)",
        description = "Modelo idêntico ao PowerPoint: tabela de 2 colunas com cabeçalho terracota e 13 linhas alternadas",
        title = "Tabela",
        content = """
| Coluna 1 | Coluna 2 |
| :--- | :--- |
| | |
| | |
| | |
| | |
| | |
| | |
| | |
| | |
| | |
| | |
| | |
| | |
| | |
        """.trimIndent(),
        color = "#c84b15",
        tags = listOf("tabela", "powerpoint", "comparativo")
    )

    val ALL = listOf(BLANK, SURGICAL_TERMINOLOGY, POWERPOINT_TABLE)
}

