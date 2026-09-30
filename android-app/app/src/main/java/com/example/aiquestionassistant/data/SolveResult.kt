package com.example.aiquestionassistant.data

data class SolveResult(
    val isSuccess: Boolean,
    val correctOption: String? = null,
    val explanation: String = "",
    val rawText: String = "",
    val error: String? = null
)
