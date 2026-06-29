package com.example.model

data class YogaPose(
    val id: Int,
    val sanskritName: String,
    val englishName: String,
    val description: String,
    val benefits: String,
    val instructions: List<String>,
    val voicePrompt: String,
    val holdDurationSec: Int = 30
)

data class YogaFlow(
    val id: String,
    val name: String,
    val description: String,
    val difficulty: String,
    val totalDurationMinutes: Int,
    val poses: List<YogaPose>
)