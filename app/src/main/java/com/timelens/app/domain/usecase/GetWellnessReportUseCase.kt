package com.timelens.app.domain.usecase

import com.timelens.app.domain.model.*
import com.timelens.app.util.TimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt

class GetWellnessReportUseCase @Inject constructor() {

    operator fun invoke(
        summary: DaySummary,
        yesterday: DaySummary?,
        goalHours: Int,
        isNightReview: Boolean = false
    ): WellnessReport {
        val insights = mutableListOf<WellnessInsight>()
        val goalMs = goalHours * 3600000L

        // 1. Regla de Pantalla vs Objetivo
        if (goalMs > 0 && summary.totalScreenTimeMs >= goalMs) {
            val overtimeMs = summary.totalScreenTimeMs - goalMs
            val overtimeFormatted = TimeFormatter.formatMillisToShort(overtimeMs)
            val action = if (isNightReview) {
                "Cerrá la jornada apagando las pantallas para evitar fatiga acumulada antes de dormir."
            } else {
                "Para lo que resta del día, priorizá actividades sin pantallas (caminata, lectura física o desconexión)."
            }
            insights.add(
                WellnessInsight(
                    id = "goal_exceeded",
                    title = "Objetivo diario superado",
                    message = "Alcanzaste ${TimeFormatter.formatMillisToShort(summary.totalScreenTimeMs)} de uso hoy, superando tu meta por $overtimeFormatted.",
                    actionTip = action,
                    level = InsightLevel.CRITICAL,
                    category = InsightCategory.SCREEN_TIME
                )
            )
        } else if (goalMs > 0 && summary.totalScreenTimeMs in 1L..(goalMs * 0.7).toLong()) {
            val remainingFormatted = TimeFormatter.formatMillisToShort(goalMs - summary.totalScreenTimeMs)
            insights.add(
                WellnessInsight(
                    id = "goal_healthy",
                    title = "Excelente autocontrol de pantalla",
                    message = "Llevás ${TimeFormatter.formatMillisToShort(summary.totalScreenTimeMs)} de uso, con un margen de $remainingFormatted disponibles.",
                    actionTip = "Mantené este ritmo consciente para no sobrecargar tu atención.",
                    level = InsightLevel.EXCELLENT,
                    category = InsightCategory.SCREEN_TIME
                )
            )
        } else if (goalMs > 0) {
            insights.add(
                WellnessInsight(
                    id = "goal_moderate",
                    title = "Ritmo de pantalla moderado",
                    message = "Estás en ${TimeFormatter.formatMillisToShort(summary.totalScreenTimeMs)} de tu objetivo de ${goalHours}h.",
                    actionTip = "Pausas breves de 5 minutos cada hora ayudan a despejar la mente y prevenir el cansancio.",
                    level = InsightLevel.MODERATE,
                    category = InsightCategory.SCREEN_TIME
                )
            )
        }

        // 2. Regla de Desbloqueos Compulsivos
        if (summary.totalUnlocks >= 65) {
            insights.add(
                WellnessInsight(
                    id = "unlocks_high",
                    title = "Frecuencia alta de desbloqueos (${summary.totalUnlocks} veces)",
                    message = "Desbloqueaste el móvil ${summary.totalUnlocks} veces hoy. Tantas aperturas revelan micro-distracciones automáticas e hiperconexión constante.",
                    actionTip = "Silenciá notificaciones no esenciales y probá dejar el teléfono fuera del campo visual para proteger tu foco.",
                    level = InsightLevel.ATTENTION,
                    category = InsightCategory.UNLOCKS
                )
            )
        } else if (summary.totalUnlocks in 35..64) {
            insights.add(
                WellnessInsight(
                    id = "unlocks_moderate",
                    title = "Desbloqueos en nivel medio (${summary.totalUnlocks} veces)",
                    message = "Registraste ${summary.totalUnlocks} desbloqueos hoy, revisando el teléfono con cierta regularidad a lo largo del día.",
                    actionTip = "Agrupá la revisión de mensajes en bloques horarios fijos (ej: cada 45 minutos) en vez de chequear continuamente.",
                    level = InsightLevel.MODERATE,
                    category = InsightCategory.UNLOCKS
                )
            )
        } else if (summary.totalUnlocks in 1..34 && summary.totalScreenTimeMs > 0) {
            insights.add(
                WellnessInsight(
                    id = "unlocks_low",
                    title = "Alta presencia mental (${summary.totalUnlocks} desbloqueos)",
                    message = "Solo registraste ${summary.totalUnlocks} interrupciones hoy, demostrando gran enfoque en tus tareas.",
                    actionTip = "Tu concentración sostenida es el hábito más valioso contra la dispersión digital.",
                    level = InsightLevel.EXCELLENT,
                    category = InsightCategory.UNLOCKS
                )
            )
        }

        // 3. Regla de Sesión Continua y Fatiga Visual
        val longest = summary.longestSession
        if (longest != null && longest.durationMs >= 60 * 60 * 1000L) {
            val sessionFormatted = TimeFormatter.formatMillisToShort(longest.durationMs)
            insights.add(
                WellnessInsight(
                    id = "session_marathon",
                    title = "Sesión intensiva en ${longest.appName}",
                    message = "Pasaste $sessionFormatted continuos en ${longest.appName}. Una hora ininterrumpida fatiga el cerebro y la vista.",
                    actionTip = "Implementá la técnica Pomodoro (bloques de 25 min) y realizá estiramientos físicos entre descansos.",
                    level = InsightLevel.ATTENTION,
                    category = InsightCategory.SESSION_LENGTH
                )
            )
        } else if (longest != null && longest.durationMs >= 35 * 60 * 1000L) {
            val sessionFormatted = TimeFormatter.formatMillisToShort(longest.durationMs)
            insights.add(
                WellnessInsight(
                    id = "session_long",
                    title = "Sesión prolongada en ${longest.appName}",
                    message = "Registraste $sessionFormatted seguidos en ${longest.appName}. Fijar la vista tanto tiempo reduce el parpadeo natural.",
                    actionTip = "Regla 20-20-20: cada 20 minutos, mirá un punto a 6 metros de distancia durante 20 segundos.",
                    level = InsightLevel.ATTENTION,
                    category = InsightCategory.SESSION_LENGTH
                )
            )
        } else if (longest != null && longest.durationMs in 1L..(20 * 60 * 1000L) && summary.totalScreenTimeMs >= 30 * 60 * 1000L) {
            insights.add(
                WellnessInsight(
                    id = "session_healthy",
                    title = "Sesiones breves y dosificadas",
                    message = "Tu sesión más extensa fue de solo ${TimeFormatter.formatMillisToShort(longest.durationMs)} en ${longest.appName}.",
                    actionTip = "Evitaste quedar atrapado en consumos prolongados e involuntarios. ¡Excelente disciplina!",
                    level = InsightLevel.EXCELLENT,
                    category = InsightCategory.SESSION_LENGTH
                )
            )
        }

        // 4. Regla de Balance por Categorías
        val socialApps = summary.topApps.filter { (it.category ?: AppCategory.OTHER) == AppCategory.SOCIAL }
        val socialTimeMs = socialApps.sumOf { it.totalTimeMs }
        val topSocial = socialApps.maxByOrNull { it.totalTimeMs }

        if (socialTimeMs >= 35 * 60 * 1000L && summary.totalScreenTimeMs > 0) {
            val ratio = socialTimeMs.toFloat() / summary.totalScreenTimeMs
            if (ratio >= 0.35f) {
                val percent = (ratio * 100).roundToInt()
                insights.add(
                    WellnessInsight(
                        id = "social_balance",
                        title = "Uso de Redes Sociales (${TimeFormatter.formatMillisToShort(socialTimeMs)})",
                        message = "El $percent% de tu pantalla fue en redes sociales, principalmente en ${topSocial?.appName ?: "redes"}.",
                        actionTip = "Configurá un límite diario de 25 min en ${topSocial?.appName ?: "redes"} para transformar el scrolling automático en consumo consciente.",
                        level = if (ratio >= 0.55f) InsightLevel.ATTENTION else InsightLevel.MODERATE,
                        category = InsightCategory.APP_BALANCE
                    )
                )
            }
        }

        val gameApps = summary.topApps.filter { (it.category ?: AppCategory.OTHER) == AppCategory.GAMING }
        val gamingTimeMs = gameApps.sumOf { it.totalTimeMs }
        val topGame = gameApps.maxByOrNull { it.totalTimeMs }
        if (gamingTimeMs >= 30 * 60 * 1000L) {
            insights.add(
                WellnessInsight(
                    id = "gaming_balance",
                    title = "Sesiones de Juego (${TimeFormatter.formatMillisToShort(gamingTimeMs)})",
                    message = "Dedicaste tiempo de ocio a juegos (${topGame?.appName ?: "juegos"}).",
                    actionTip = "Hacé estiramientos de cuello, muñecas y hombros para cuidar tu postura ergonómica.",
                    level = InsightLevel.MODERATE,
                    category = InsightCategory.APP_BALANCE
                )
            )
        }

        val entApps = summary.topApps.filter { (it.category ?: AppCategory.OTHER) == AppCategory.ENTERTAINMENT }
        val entTimeMs = entApps.sumOf { it.totalTimeMs }
        val topEnt = entApps.maxByOrNull { it.totalTimeMs }
        if (entTimeMs >= 45 * 60 * 1000L) {
            insights.add(
                WellnessInsight(
                    id = "entertainment_balance",
                    title = "Streaming y Videos (${TimeFormatter.formatMillisToShort(entTimeMs)})",
                    message = "Consumiste contenido audiovisual en ${topEnt?.appName ?: "plataformas de video"}.",
                    actionTip = "El contenido audiovisual distiende, pero evitá consumirlo a oscuras o justo antes de dormir.",
                    level = InsightLevel.MODERATE,
                    category = InsightCategory.APP_BALANCE
                )
            )
        }

        val prodApps = summary.topApps.filter {
            it.category == AppCategory.PRODUCTIVITY || it.category == AppCategory.COMMUNICATION
        }
        val prodTimeMs = prodApps.sumOf { it.totalTimeMs }
        if (summary.totalScreenTimeMs > 0 && prodTimeMs >= 40 * 60 * 1000L) {
            val prodRatio = prodTimeMs.toFloat() / summary.totalScreenTimeMs
            if (prodRatio >= 0.45f) {
                insights.add(
                    WellnessInsight(
                        id = "productivity_focus",
                        title = "Uso Productivo y Funcional",
                        message = "Dedicaste ${TimeFormatter.formatMillisToShort(prodTimeMs)} (${(prodRatio * 100).roundToInt()}%) a productividad y comunicación.",
                        actionTip = "Herramientas usadas con propósito. Recordá tomar descansos posturales periódicos.",
                        level = InsightLevel.EXCELLENT,
                        category = InsightCategory.APP_BALANCE
                    )
                )
            }
        }

        // 5. Regla de Comparación con Ayer
        if (yesterday != null && yesterday.totalScreenTimeMs > 0) {
            val diffMs = summary.totalScreenTimeMs - yesterday.totalScreenTimeMs
            val diffPercent = (diffMs.toFloat() / yesterday.totalScreenTimeMs) * 100
            if (diffPercent <= -15f) {
                insights.add(
                    WellnessInsight(
                        id = "progress_yesterday",
                        title = "Reducción de pantalla (-${abs(diffPercent).roundToInt()}%)",
                        message = "Usaste el teléfono ${TimeFormatter.formatMillisToShort(abs(diffMs))} menos que ayer a esta hora.",
                        actionTip = "¡Gran progreso! Consolidar hábitos más conscientes te permite ganar tiempo propio de calidad.",
                        level = InsightLevel.EXCELLENT,
                        category = InsightCategory.SCREEN_TIME
                    )
                )
            } else if (diffPercent >= 35f && summary.totalScreenTimeMs >= 90 * 60 * 1000L) {
                insights.add(
                    WellnessInsight(
                        id = "increase_yesterday",
                        title = "Aumento de pantalla (+${diffPercent.roundToInt()}%)",
                        message = "Tu tiempo en pantalla subió ${TimeFormatter.formatMillisToShort(diffMs)} frente a ayer.",
                        actionTip = "Observá qué apps generaron el pico para distinguir tareas necesarias de ocio desmedido.",
                        level = InsightLevel.ATTENTION,
                        category = InsightCategory.SCREEN_TIME
                    )
                )
            }
        }

        // 6. Regla Nocturna / Cierre de las 22:00 hs
        if (isNightReview) {
            val formattedScreen = TimeFormatter.formatMillisToShort(summary.totalScreenTimeMs)
            val nightAction = if (summary.totalScreenTimeMs > goalMs) {
                "Superaste tu meta diaria; es hora de apagar pantallas, preparar tu espacio de descanso y desconectar."
            } else {
                "¡Gran cierre de jornada! Dejá el teléfono cargando fuera del alcance de la cama para optimizar tu descanso."
            }
            insights.add(
                0, // Prioridad en revisión nocturna
                WellnessInsight(
                    id = "night_closing_review",
                    title = "Revisión Nocturna de Cierre (22:00 hs)",
                    message = "Completaste el día con $formattedScreen de pantalla y ${summary.totalUnlocks} desbloqueos.",
                    actionTip = nightAction,
                    level = if (summary.totalScreenTimeMs > goalMs) InsightLevel.ATTENTION else InsightLevel.EXCELLENT,
                    category = InsightCategory.NIGHT_USAGE
                )
            )
        } else {
            if (summary.peakHour in 21..23 || summary.peakHour in 0..4) {
                val hourFormatted = String.format(Locale.getDefault(), "%02d:00", summary.peakHour)
                insights.add(
                    WellnessInsight(
                        id = "night_peak",
                        title = "Pico de actividad nocturna ($hourFormatted hs)",
                        message = "Tu mayor consumo de pantalla se concentró en horario nocturno.",
                        actionTip = "La luz azul inhibe la melatonina. Si usás el teléfono de noche, activá la luz cálida de lectura.",
                        level = InsightLevel.ATTENTION,
                        category = InsightCategory.NIGHT_USAGE
                    )
                )
            }
        }

        // 7. Regla de Franja Más Productiva (Mayor Enfoque)
        if (summary.totalScreenTimeMs > 0 && summary.productiveHour in 8..21) {
            val prodHourFormatted = String.format(Locale.getDefault(), "%02d:00", summary.productiveHour)
            insights.add(
                WellnessInsight(
                    id = "productive_focus_hour",
                    title = "Franja de mayor enfoque ($prodHourFormatted hs)",
                    message = "A las $prodHourFormatted hs registraste el menor uso del teléfono en tu día, manteniendo desconexión y presencia mental.",
                    actionTip = "Aprovechá este horario de bajo uso digital para programar tus tareas de mayor complejidad o estudio.",
                    level = InsightLevel.EXCELLENT,
                    category = InsightCategory.APP_BALANCE
                )
            )
        }

        // Ordenamiento de Insights (CRITICAL -> ATTENTION -> EXCELLENT -> MODERATE)
        val sortedInsights = insights.sortedBy { insight ->
            if (isNightReview && insight.id == "night_closing_review") -1
            else when (insight.level) {
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
            actionTip = "Utilizá tu teléfono de forma consciente a lo largo de tu día.",
            level = InsightLevel.EXCELLENT,
            category = InsightCategory.SCREEN_TIME
        )

        // Cálculo de Score General (0 a 100)
        var score = 100
        if (goalMs > 0 && summary.totalScreenTimeMs > goalMs) score -= 35
        if (summary.totalUnlocks > 65) score -= 20
        else if (summary.totalUnlocks > 40) score -= 10
        if (longest != null && longest.durationMs >= 45 * 60 * 1000L) score -= 20
        if (summary.peakHour in 21..23 || summary.peakHour in 0..4) score -= 15
        score = score.coerceIn(15, 100)

        val overallStatus = when {
            score >= 80 -> "Hábitos Saludables"
            score >= 60 -> "Uso Equilibrado"
            score >= 40 -> "Atención al Hábito"
            else -> "Límite Excedido"
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
