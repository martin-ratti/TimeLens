package com.timelens.app.domain.model

import java.time.LocalDate

enum class InsightLevel {
    EXCELLENT,  // Verde neón: Logro o gran autocontrol
    MODERATE,   // Azul neón: Hábito regular con sugerencias constructivas
    ATTENTION,  // Naranja neón: Alerta preventiva sobre hábito distractor
    CRITICAL    // Rojo neón: Límite superado o uso continuo desmedido
}

enum class InsightCategory {
    SCREEN_TIME,
    UNLOCKS,
    SESSION_LENGTH,
    NIGHT_USAGE,
    APP_BALANCE
}

data class WellnessInsight(
    val id: String,
    val title: String,
    val message: String,
    val actionTip: String,
    val level: InsightLevel,
    val category: InsightCategory
)

data class WellnessReport(
    val date: LocalDate,
    val overallScore: Int,
    val overallStatus: String,
    val primaryInsight: WellnessInsight,
    val allInsights: List<WellnessInsight>
)
