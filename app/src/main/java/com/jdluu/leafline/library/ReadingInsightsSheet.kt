package com.jdluu.leafline.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jdluu.leafline.LibraryViewModel
import com.jdluu.leafline.library.data.ReadingAggregate
import com.jdluu.leafline.theme.Spacing
import java.time.LocalDate

@Composable
private fun InsightMetric(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    emphasised: Boolean = true
) {
    Column(modifier = modifier.semantics(mergeDescendants = true) {}) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            color = if (emphasised) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReadingInsightsSheet(
    viewModel: LibraryViewModel,
    onDismiss: () -> Unit
) {
    val readingGoals by viewModel.readingGoals.collectAsState()
    val allBooks by viewModel.books.collectAsState()
    val finishedBooksCount = remember(allBooks) { allBooks.count { it.readingStatus == ReadingStatus.FINISHED } }
    var showEditGoalsDialog by remember { mutableStateOf(false) }

    var todayStats by remember { mutableStateOf(ReadingAggregate.EMPTY) }
    var weeklyStats by remember { mutableStateOf(ReadingAggregate.EMPTY) }
    var recentDailyStats by remember { mutableStateOf<List<Pair<LocalDate, ReadingAggregate>>>(emptyList()) }
    var selectedDayIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) {
        todayStats = viewModel.getTodayAggregate()
        weeklyStats = viewModel.getWeeklyAggregate()
        recentDailyStats = viewModel.getRecentDailyAggregates(7)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .padding(horizontal = Spacing.md, vertical = Spacing.sm)
                .padding(bottom = Spacing.lg)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Reading Insights",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "100% private and on-device. No analytics or tracking.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(Spacing.md))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    Text(
                        text = "Today",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(Modifier.height(Spacing.xs))
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            InsightMetric(
                                value = formatReadingDuration(todayStats.activeDurationMillis),
                                label = "Time read",
                                modifier = Modifier.weight(1f)
                            )
                            InsightMetric(
                                value = "${todayStats.sessionCount}",
                                label = "Sessions",
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            InsightMetric(
                                value = "${todayStats.booksOpened}",
                                label = "Books opened",
                                modifier = Modifier.weight(1f)
                            )
                            InsightMetric(
                                value = formatReadingDuration(
                                    if (todayStats.sessionCount > 0) {
                                        todayStats.activeDurationMillis / todayStats.sessionCount
                                    } else {
                                        0L
                                    }
                                ),
                                label = "Avg session",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(Spacing.md))

            val todayMinutes = todayStats.activeMinutes
            val dailyTarget = readingGoals.dailyMinutes
            val dailyProgress = if (dailyTarget > 0) (todayMinutes.toFloat() / dailyTarget.toFloat()).coerceIn(0f, 1f) else 0f
            val dailyGoalReached = todayMinutes >= dailyTarget

            val booksTarget = readingGoals.booksTarget
            val booksProgress = if (booksTarget > 0) (finishedBooksCount.toFloat() / booksTarget.toFloat()).coerceIn(0f, 1f) else 0f
            val booksGoalReached = finishedBooksCount >= booksTarget

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(Spacing.md)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Reading Goals",
                            style = MaterialTheme.typography.titleMedium
                        )
                        TextButton(
                            onClick = { showEditGoalsDialog = true },
                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        ) {
                            Text("Edit")
                        }
                    }

                    Spacer(Modifier.height(Spacing.xs))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Daily reading target",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$todayMinutes / $dailyTarget min",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (dailyGoalReached) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(vertical = Spacing.xs)
                            ) {
                                Text(
                                    text = "Goal met! 🎉",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = Spacing.xs, vertical = 2.dp)
                                )
                            }
                        } else {
                            Text(
                                text = "${(dailyProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(Modifier.height(Spacing.xs))
                    LinearProgressIndicator(
                        progress = { dailyProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(MaterialTheme.shapes.small),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface
                    )

                    Spacer(Modifier.height(Spacing.md))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Books finished target",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$finishedBooksCount / $booksTarget books",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        if (booksGoalReached) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.padding(vertical = Spacing.xs)
                            ) {
                                Text(
                                    text = "Goal met! 🏆",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = Spacing.xs, vertical = 2.dp)
                                )
                            }
                        } else {
                            Text(
                                text = "${(booksProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(Modifier.height(Spacing.xs))
                    LinearProgressIndicator(
                        progress = { booksProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(MaterialTheme.shapes.small),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surface
                    )
                }
            }

            Spacer(Modifier.height(Spacing.md))

            Text(
                text = "Past 7 Days",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "Total: ${formatReadingDuration(weeklyStats.activeDurationMillis)} across ${weeklyStats.sessionCount} sessions",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(Spacing.md))

            AnimatedVisibility(visible = selectedDayIndex != null) {
                selectedDayIndex?.let { idx ->
                    if (idx in recentDailyStats.indices) {
                        val (selectedDate, selectedAgg) = recentDailyStats[idx]
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = Spacing.xs)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = Spacing.md, vertical = Spacing.xs),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${selectedDate.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }}, ${selectedDate.month.name.lowercase().replaceFirstChar { it.uppercase() }} ${selectedDate.dayOfMonth}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = "${formatReadingDuration(selectedAgg.activeDurationMillis)} • ${selectedAgg.sessionCount} session${if (selectedAgg.sessionCount == 1) "" else "s"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            val maxMinutes = (recentDailyStats.maxOfOrNull { it.second.activeMinutes } ?: 0L).coerceAtLeast(15L)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(Spacing.sm),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                recentDailyStats.forEachIndexed { index, (date, agg) ->
                    val isSelected = selectedDayIndex == index
                    val isToday = date == LocalDate.now()
                    val targetRatio = (agg.activeMinutes.toFloat() / maxMinutes.toFloat()).coerceIn(0.06f, 1f)
                    val animatedRatio by animateFloatAsState(
                        targetValue = targetRatio,
                        animationSpec = tween(durationMillis = 400),
                        label = "chartBarHeight"
                    )
                    val dayLabel = date.dayOfWeek.name.take(3)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(MaterialTheme.shapes.small)
                            .clickable {
                                selectedDayIndex = if (selectedDayIndex == index) null else index
                            }
                            .padding(vertical = 4.dp)
                            .semantics {
                                contentDescription = "$dayLabel: ${agg.activeMinutes} minutes, ${agg.sessionCount} sessions"
                            }
                    ) {
                        Text(
                            text = if (agg.activeMinutes > 0) "${agg.activeMinutes}m" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(if (isSelected) 20.dp else 16.dp)
                                .height((70 * animatedRatio).dp)
                                .clip(MaterialTheme.shapes.small)
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primary
                                        agg.activeMinutes > 0 -> MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                        else -> MaterialTheme.colorScheme.outlineVariant
                                    }
                                )
                                .then(
                                    if (isToday) Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
                                    else Modifier
                                )
                        )
                        Spacer(Modifier.height(Spacing.xs))
                        Text(
                            text = dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.md))
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Close")
            }
        }

        if (showEditGoalsDialog) {
            EditReadingGoalsDialog(
                initialDailyMinutes = readingGoals.dailyMinutes,
                initialBooksTarget = readingGoals.booksTarget,
                onDismiss = { showEditGoalsDialog = false },
                onSave = { dailyMins, booksTarget ->
                    viewModel.updateReadingGoals(dailyMins, booksTarget)
                }
            )
        }
    }
}

@Composable
private fun EditReadingGoalsDialog(
    initialDailyMinutes: Int,
    initialBooksTarget: Int,
    onDismiss: () -> Unit,
    onSave: (dailyMinutes: Int, booksTarget: Int) -> Unit
) {
    var dailyMinutesText by remember { mutableStateOf(initialDailyMinutes.toString()) }
    var booksTargetText by remember { mutableStateOf(initialBooksTarget.toString()) }

    val parsedDailyMinutes = dailyMinutesText.trim().toIntOrNull()
    val parsedBooksTarget = booksTargetText.trim().toIntOrNull()
    val isValid = parsedDailyMinutes != null && parsedDailyMinutes > 0 &&
        parsedBooksTarget != null && parsedBooksTarget > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Reading Goals",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm)
            ) {
                Text(
                    text = "Set your personal reading targets. All goals and stats remain entirely private on your device.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = dailyMinutesText,
                    onValueChange = { dailyMinutesText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Daily target (minutes)") },
                    singleLine = true,
                    isError = parsedDailyMinutes == null || parsedDailyMinutes <= 0,
                    modifier = Modifier.fillMaxWidth()
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    listOf(15, 20, 30, 45, 60).forEach { mins ->
                        FilterChip(
                            selected = parsedDailyMinutes == mins,
                            onClick = { dailyMinutesText = mins.toString() },
                            label = { Text("${mins}m") },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        )
                    }
                }

                Spacer(Modifier.height(Spacing.xs))

                OutlinedTextField(
                    value = booksTargetText,
                    onValueChange = { booksTargetText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Books target") },
                    singleLine = true,
                    isError = parsedBooksTarget == null || parsedBooksTarget <= 0,
                    modifier = Modifier.fillMaxWidth()
                )

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
                    verticalArrangement = Arrangement.spacedBy(Spacing.xs)
                ) {
                    listOf(5, 12, 24, 52).forEach { count ->
                        FilterChip(
                            selected = parsedBooksTarget == count,
                            onClick = { booksTargetText = count.toString() },
                            label = { Text("$count") },
                            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (parsedDailyMinutes != null && parsedBooksTarget != null && isValid) {
                        onSave(parsedDailyMinutes, parsedBooksTarget)
                        onDismiss()
                    }
                },
                enabled = isValid,
                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
            ) {
                Text("Cancel")
            }
        }
    )
}
