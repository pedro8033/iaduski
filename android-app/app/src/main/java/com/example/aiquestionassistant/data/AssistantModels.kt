package com.example.aiquestionassistant.data

enum class AssistantActionType(val id: String, val label: String, val icon: String) {
    SUMMARIZE("summarize", "Resumir", "description"),
    EXPLAIN("explain", "Explicar", "lightbulb"),
    DIAGRAM("diagram", "Criar Diagrama", "account_tree"),
    IMAGE("image", "Gerar Imagem", "image"),
    TRANSLATE("translate", "Traduzir", "translate"),
    FLASHCARDS("flashcards", "Flashcards", "style"),
    SOLVE("solve", "Resolver Questão", "check_circle"),
    ASK("ask", "Perguntar", "chat")
}

data class AssistantActionOptions(
    val detailLevel: String = "detailed", // short, detailed, bullet_points, simple
    val targetLanguage: String = "Português",
    val diagramType: String = "flowchart", // flowchart, mindmap, process, hierarchy
    val imageStyle: String = "educational", // educational, infographic, realistic, schematic, 3d
    val audienceLevel: String = "intermediate" // beginner, intermediate, expert
)

data class AssistantResult(
    val id: String = java.util.UUID.randomUUID().toString(),
    val isSuccess: Boolean,
    val action: AssistantActionType,
    val title: String = "",
    val resultText: String = "",
    val diagramCode: String? = null,
    val imageUrl: String? = null,
    val keyPoints: List<String> = emptyList(),
    val error: String? = null,
    val latencyMs: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
)
