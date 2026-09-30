package com.timelens.app.domain.usecase

import com.timelens.app.domain.model.*
import com.timelens.app.util.TimeFormatter
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt

class GetWellnessReportUseCase @Inject constructor() {

    operator fun invoke(
        summary: DaySummary,
        yesterday: DaySummary?,
        goalHours: Int
    ): WellnessReport {
        val insights = mutableListOf<WellnessInsight>()
        val goalMs = goalHours * 3600000L

        // 1. Regla de Tiempo de Pantalla vs Objetivo
        if (goalMs > 0 && summary.totalScreenTimeMs >= goalMs) {
            val overtimeMs = summary.totalScreenTimeMs - goalMs
            val overtimeFormatted = TimeFormatter.formatMillisToShort(overtimeMs)
            insights.add(
                WellnessInsight(
                    id = "goal_exceeded",
                    title = "Objetivo diario superado",
                    message = "Llegaste a ${TimeFormatter.formatMillisToShort(summary.totalScreenTimeMs)} de uso hoy, superando tu meta por $overtimeFormatted.",
                    actionTip = "Considerá activar el modo 'No Molestar' y desconectar pantallas durante la noche.",
                    level = InsightLevel.CRITICAL,
                    category = InsightCategory.SCREEN_TIME
                )
            )
        } else if (goalMs > 0 && summary.totalScreenTimeMs in 1L..(goalMs * 0.7).toLong()) {
            val remainingFormatted = TimeFormatter.formatMillisToShort(goalMs - summary.totalScreenTimeMs)
            insights.add(
                WellnessInsight(
                    id = "goal_healthy",
                    title = "¡Excelente autocontrol hoy!",
                    message = "Llevás ${TimeFormatter.formatMillisToShort(summary.totalScreenTimeMs)} de uso, aún tenés $remainingFormatted disponibles antes de tu límite.",
                    actionTip = "¡Mantené el foco en tus metas y actividades fuera de la pantalla!",
                    level = InsightLevel.EXCELLENT,
                    category = InsightCategory.SCREEN_TIME
                )
            )
        } else {
            insights.add(
                WellnessInsight(
                    id = "goal_moderate",
                    title = "Ritmo de uso moderado",
                    message = "Estás dentro del tiempo esperado (${TimeFormatter.formatMillisToShort(summary.totalScreenTimeMs)} de ${goalHours}h).",
                    actionTip = "Hacer pausas breves de 5 minutos cada hora mantiene despejada tu mente.",
                    level = InsightLevel.MODERATE,
                    category = InsightCategory.SCREEN_TIME
                )
            )
        }

        // 2. Regla de Desbloqueos Compulsivos
        if (summary.totalUnlocks >= 60) {
            insights.add(
                WellnessInsight(
                    id = "unlocks_high",
                    title = "Frecuencia alta de desbloqueos",
                    message = "Desbloqueaste el teléfono ${summary.totalUnlocks} veces hoy. Muchas aperturas suelen ser micro-distracciones automáticas.",
                    actionTip = "Intentá agrupar consultas cada 30 o 60 minutos en lugar de revisar el móvil continuamente.",
                    level = InsightLevel.ATTENTION,
                    category = InsightCategory.UNLOCKS
                )
            )
        } else if (summary.totalUnlocks in 1..35) {
            insights.add(
                WellnessInsight(
                    id = "unlocks_low",
                    title = "Pocas interrupciones",
                    message = "Solo registraste ${summary.totalUnlocks} desbloqueos hoy, demostrando gran presencia y concentración en tus tareas.",
                    actionTip = "Tu capacidad de atención sostenida es clave para tu productividad.",
                    level = InsightLevel.EXCELLENT,
                    category = InsightCategory.UNLOCKS
                )
            )
        }

        // 3. Regla de Sesión Continua Prolongada
        val longest = summary.longestSession
        if (longest != null && longest.durationMs >= 40 * 60 * 1000L) {
            val sessionFormatted = TimeFormatter.formatMillisToShort(longest.durationMs)
            insights.add(
                WellnessInsight(
                    id = "long_session",
                    title = "Sesión maratónica en ${longest.appName}",
                    message = "Pasaste $sessionFormatted de forma ininterrumpida en ${longest.appName}. La mirada fija continua incrementa la fatiga ocular.",
                    actionTip = "Regla 20-20-20: cada 20 minutos, mirá un punto a 6 metros de distancia durante 20 segundos.",
                    level = InsightLevel.ATTENTION,
                    category = InsightCategory.SESSION_LENGTH
                )
            )
        }

        // 4. Regla de Comparación con Ayer
        if (yesterday != null && yesterday.totalScreenTimeMs > 0) {
            val diffMs = summary.totalScreenTimeMs - yesterday.totalScreenTimeMs
            val diffPercent = (diffMs.toFloat() / yesterday.totalScreenTimeMs) * 100
            if (diffPercent <= -15f) {
                insights.add(
                    WellnessInsight(
                        id = "progress_yesterday",
                        title = "Notable reducción de pantalla",
                        message = "Redujiste un ${abs(diffPercent).roundToInt()}% tu tiempo de pantalla frente a ayer. ¡Gran avance en tu desconexión digital!",
                        actionTip = "Disfrutá este tiempo recuperado para descansar o compartir con amigos.",
                        level = InsightLevel.EXCELLENT,
                        category = InsightCategory.SCREEN_TIME
                    )
                )
            }
        }

        // 5. Regla de Actividad Nocturna
        if (summary.peakHour in 21..23 || summary.peakHour in 0..4) {
            insights.add(
                WellnessInsight(
                    id = "night_usage",
                    title = "Horario pico nocturno (${String.format(java.util.Locale.getDefault(), "%02d:00", summary.peakHour)} hs)",
                    message = "Tu mayor consumo de pantalla ocurrió en horas de la noche. La luz azul retrasa la liberación de melatonina y altera el descanso profundo.",
                    actionTip = "Activá la luz nocturna cálida o apagá el teléfono 30 minutos antes de dormir.",
                    level = InsightLevel.ATTENTION,
                    category = InsightCategory.NIGHT_USAGE
                )
            )
        }

        // 6. Regla de Balance de Apps (Social)
        val socialTimeMs = summary.topApps
            .filter { (it.category ?: AppCategory.OTHER) == AppCategory.SOCIAL }
            .sumOf { it.totalTimeMs }

        if (summary.totalScreenTimeMs > 0) {
            val socialRatio = socialTimeMs.toFloat() / summary.totalScreenTimeMs
            if (socialRatio >= 0.5f && summary.totalScreenTimeMs >= 45 * 60 * 1000L) {
                insights.add(
                    WellnessInsight(
                        id = "social_dominance",
                        title = "Foco en Redes Sociales (${(socialRatio * 100).roundToInt()}%)",
                        message = "Más de la mitad de tu tiempo en pantalla fue en redes sociales (${TimeFormatter.formatMillisToShort(socialTimeMs)}).",
                        actionTip = "¿Lograste dedicarle tiempo a tus metas prioritarias de hoy?",
                        level = InsightLevel.MODERATE,
                        category = InsightCategory.APP_BALANCE
                    )
                )
            }
        }

        // Selección del Insight Primario (prioriza CRITICAL -> ATTENTION -> EXCELLENT -> MODERATE)
        val sortedInsights = insights.sortedBy { insight ->
            when (insight.level) {
                InsightLevel.CRITICAL -> 0
                InsightLevel.ATTENTION -> 1
                InsightLevel.EXCELLENT -> 2
                InsightLevel.MODERATE -> 3
            }
        }

        val primary = sortedInsights.firstOrNull() ?: WellnessInsight(
            id = "default",
            title = "Día sin actividad registrada",
            message = "Aún no registraste actividad de pantalla hoy.",
            actionTip = "Utilizá tu teléfono de forma consciente.",
            level = InsightLevel.EXCELLENT,
            category = InsightCategory.SCREEN_TIME
        )

        // Cálculo de Score General (0 a 100)
        var score = 100
        if (goalMs > 0 && summary.totalScreenTimeMs > goalMs) score -= 35
        if (summary.totalUnlocks > 60) score -= 20
        if (longest != null && longest.durationMs >= 45 * 60 * 1000L) score -= 20
        if (summary.peakHour in 21..23 || summary.peakHour in 0..4) score -= 15
        score = score.coerceIn(15, 100)

        val overallStatus = when {
            score >= 80 -> "🌿 Hábitos Saludables"
            score >= 60 -> "⚖️ Uso Equilibrado"
            score >= 40 -> "⚠️ Atención al Hábito"
            else -> "🎯 Límite Excedido"
        }

        return WellnessReport(
            date = summary.date,
            overallScore = score,
            overallStatus = overallStatus,
            primaryInsight = primary,
            allInsights = sortedInsights
        )
    }
}
