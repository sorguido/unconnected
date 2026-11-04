package it.sorguido.unconnected

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TimerScreen(
    remainingMillis: Long,
    busy: Boolean,
    permissionsReady: Boolean,
    sessionPaused: Boolean,
    onBack: () -> Unit,
    onStart: (Long) -> Unit,
    onStop: () -> Unit,
) {
    var digits by rememberSaveable { mutableStateOf("") }
    var wasActive by rememberSaveable { mutableStateOf(remainingMillis > 0) }
    val active = remainingMillis > 0
    val committed = timerStatus(active, permissionsReady, sessionPaused)
    var held by remember { mutableStateOf(committed) }
    SideEffect { if (!busy) held = committed }
    SideEffect { if (active) wasActive = true }
    BackHandler(onBack = onBack)
    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(Modifier.widthIn(max = 520.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                TextButton(onClick = onBack) { Text(stringResource(R.string.timer_back)) }
                Text(stringResource(R.string.timer_title), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.timer_help))
                if (active) {
                    val status = cardStatus(committed, busy, held)
                    Text(stringResource(status.title), style = MaterialTheme.typography.titleLarge)
                    Text(stringResource(status.detail))
                    CountdownText(remainingMillis)
                    Text(stringResource(R.string.timer_schedule_return), style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = onStop, enabled = !busy, colors = toggleButtonColors(true, busy), modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
                        Text(stringResource(R.string.deactivate_timer))
                    }
                } else {
                    if (wasActive) Text(stringResource(R.string.timer_finished), color = MaterialTheme.colorScheme.primary)
                    val padded = digits.padStart(6, '0')
                    // The three labelled fields wrap at large font scales or in a narrow window.
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        listOf(R.string.timer_hours, R.string.timer_minutes, R.string.timer_seconds).forEachIndexed { index, label ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(padded.substring(index * 2, index * 2 + 2), style = MaterialTheme.typography.displayMedium)
                                Text(stringResource(label), style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                    Text(stringResource(R.string.timer_entry_help), style = MaterialTheme.typography.bodyMedium)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(1, 5, 10, 15).forEach { minutes ->
                            SuggestionChip(onClick = { digits = (minutes * 100).toString() }, enabled = !busy,
                                label = { Text(stringResource(R.string.timer_quick_minutes, minutes)) })
                        }
                    }
                    listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7, 8, 9)).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { digit ->
                                OutlinedButton(onClick = { digits = appendTimerDigit(digits, digit) }, enabled = !busy && digits.length < 6,
                                    modifier = Modifier.weight(1f).heightIn(min = 56.dp), contentPadding = PaddingValues(8.dp)) {
                                    Text(digit.toString(), style = MaterialTheme.typography.headlineSmall)
                                }
                            }
                        }
                    }
                    FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { digits = "" }, enabled = !busy && digits.isNotEmpty(), modifier = Modifier.heightIn(min = 56.dp)) {
                            Text(stringResource(R.string.timer_clear))
                        }
                        OutlinedButton(onClick = { digits = appendTimerDigit(digits, 0) }, enabled = !busy && digits.length < 6,
                            modifier = Modifier.heightIn(min = 56.dp)) { Text("0", style = MaterialTheme.typography.headlineSmall) }
                        TextButton(onClick = { digits = digits.dropLast(1) }, enabled = !busy && digits.isNotEmpty(), modifier = Modifier.heightIn(min = 56.dp)) {
                            Text(stringResource(R.string.timer_backspace))
                        }
                    }
                    val duration = timerEntrySeconds(digits)
                    if (digits.isNotEmpty() && duration == null) Text(stringResource(R.string.timer_invalid), color = MaterialTheme.colorScheme.error)
                    if (!permissionsReady) Text(stringResource(R.string.timer_setup_needed), color = MaterialTheme.colorScheme.error)
                    Button(onClick = { duration?.let(onStart) }, enabled = !busy && permissionsReady && duration != null,
                        colors = toggleButtonColors(false, busy), modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
                        Text(stringResource(R.string.timer_start))
                    }
                }
            }
        }
        // Fixed viewport overlay, outside the scrolling content, matching the home save feedback.
        Box(Modifier.widthIn(max = 520.dp).fillMaxWidth().padding(horizontal = 20.dp).height(4.dp)) {
            if (busy) {
                val saving = stringResource(R.string.saving_detail)
                LinearProgressIndicator(Modifier.fillMaxWidth().semantics {
                    contentDescription = saving
                    liveRegion = LiveRegionMode.Polite
                })
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CountdownText(remainingMillis: Long) {
    val text = timerCountdown(remainingMillis)
    val description = stringResource(R.string.timer_remaining, text)
    // Each time component may wrap rather than clipping at large system font sizes.
    FlowRow(Modifier.fillMaxWidth().semantics(mergeDescendants = true) { contentDescription = description }, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        text.split(':').forEachIndexed { index, part ->
            Text(if (index < 2) "$part :" else part, style = MaterialTheme.typography.displayMedium, textAlign = TextAlign.Center)
        }
    }
}

@Composable
internal fun toggleButtonColors(active: Boolean, busy: Boolean): ButtonColors {
    val color = if (active) Color(0xFFB3261E) else Color(0xFF17613D)
    val colors = ButtonDefaults.buttonColors(containerColor = color, contentColor = Color.White)
    return if (busy) colors.copy(disabledContainerColor = color, disabledContentColor = Color.White) else colors
}
