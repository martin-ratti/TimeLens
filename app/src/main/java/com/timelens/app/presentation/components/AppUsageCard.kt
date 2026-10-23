package com.timelens.app.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.timelens.app.domain.model.AppCategory
import com.timelens.app.presentation.theme.*

private val CardShape = RoundedCornerShape(18.dp)
private val IconBoxShape = RoundedCornerShape(14.dp)
private val ProgressShape = RoundedCornerShape(3.dp)

@Composable
fun AppUsageCard(
    icon: Any?,
    appName: String,
    usageTime: String,
    progress: Float,
    accentColor: Color,
    sessionCount: Int,
    category: AppCategory? = null,
    rank: Int? = null,
    longestSessionText: String? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val clampedProgress = progress.coerceIn(0f, 1f)
    val isTopThree = rank != null && rank in 1..3
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant
    val borderColor = remember(isTopThree, accentColor, outlineVariant) {
        if (isTopThree) accentColor.copy(alpha = 0.28f) else outlineVariant.copy(alpha = 0.6f)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = CardShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(IconBoxShape)
                            .background(accentColor.copy(alpha = 0.14f))
                            .border(1.dp, accentColor.copy(alpha = 0.25f), IconBoxShape),
                        contentAlignment = Alignment.Center
                    ) {
                        when (icon) {
                            is ImageBitmap -> {
                                Image(
                                    bitmap = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            is ImageVector -> {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            else -> {
                                AsyncImage(
                                    model = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    if (isTopThree) {
                        val badgeColor = when (rank) {
                            1 -> NeonOrange
                            2 -> NeonBlue
                            else -> NeonPurple
                        }
                        Box(
                            modifier = Modifier
                                .offset(x = 4.dp, y = 4.dp)
                                .size(20.dp)
                                .background(badgeColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "#$rank",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = DarkBackground
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (sessionCount == 1) "1 apertura" else "$sessionCount aperturas",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (category != null) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = category.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = accentColor,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (!longestSessionText.isNullOrBlank()) {
                            Text(
                                text = "•",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Máx $longestSessionText",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = usageTime,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = accentColor
                    )
                    val share = (clampedProgress * 100).toInt()
                    if (share > 0) {
                        Text(
                            text = "$share% del día",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(accentColor.copy(alpha = 0.12f), ProgressShape)
            ) {
                if (clampedProgress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(clampedProgress)
                            .fillMaxHeight()
                            .background(accentColor, ProgressShape)
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D1117)
@Composable
fun AppUsageCardPreview() {
    TimeLensTheme(darkTheme = true) {
        AppUsageCard(
            icon = Icons.Outlined.CameraAlt,
            appName = "Instagram",
            usageTime = "1h 45m",
            progress = 0.8f,
            accentColor = NeonPurple,
            sessionCount = 23,
            rank = 1,
            longestSessionText = "32m"
        )
    }
}
