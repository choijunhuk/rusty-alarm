package com.example.rustyalarm.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.rustyalarm.alarm.Alarm
import com.example.rustyalarm.alarm.ChallengeType
import com.example.rustyalarm.rust.RustAlarmCore
import com.example.rustyalarm.ui.theme.GrayMuted

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlarmCard(
    alarm: Alarm,
    onToggle: (Boolean) -> Unit,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onDuplicate: () -> Unit = {},
    onShare: () -> Unit = {},
    nextTriggerMillis: Long? = null,
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    val enabled = alarm.enabled
    val alpha   = if (enabled) 1f else 0.45f
    val accent  = if (enabled) MaterialTheme.colorScheme.primary else GrayMuted
    val scheduleLabel = remember(alarm.specificDate, alarm.repeatDays) {
        alarm.specificDate?.let {
            java.text.SimpleDateFormat("M월 d일 (E)", java.util.Locale.KOREAN).format(java.util.Date(it))
        } ?: RustAlarmCore.getRepeatDaysLabel(alarm.repeatDays.toIntArray()).ifBlank { "한 번만" }
    }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { menuOpen = true },
            ),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) MaterialTheme.colorScheme.surface
                             else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (enabled) 1.dp else 0.dp,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 22.dp, bottomStart = 22.dp))
                    .background(accent.copy(alpha = alpha)),
            )

            // Main content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 18.dp, top = 16.dp, bottom = 16.dp, end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Big time — single tabular line
                Text(
                    text = RustAlarmCore.formatTime(alarm.hour, alarm.minute),
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Normal,
                    color = accent.copy(alpha = alpha),
                    lineHeight = 48.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                )

                // Title row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = alarm.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.85f),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (alarm.challengeType != ChallengeType.NONE) {
                        Icon(
                            Icons.Default.Bolt,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary.copy(alpha = alpha),
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }

                // Schedule + countdown (plain, separator-style)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = scheduleLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.6f),
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                    )
                    if (alarm.enabled && nextTriggerMillis != null) {
                        val delta = nextTriggerMillis - System.currentTimeMillis()
                        if (delta > 0) {
                            Text(
                                text = "· ${shortCountdown(delta)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                                maxLines = 1,
                            )
                        }
                    }
                }
            }

            // Right — toggle + long-press menu anchor
            Box {
                Switch(
                    checked = alarm.enabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                    ),
                    modifier = Modifier.padding(end = 16.dp),
                )
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("복제") },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, null) },
                        onClick = { menuOpen = false; onDuplicate() },
                    )
                    DropdownMenuItem(
                        text = { Text("공유") },
                        leadingIcon = { Icon(Icons.Default.Share, null) },
                        onClick = { menuOpen = false; onShare() },
                    )
                    DropdownMenuItem(
                        text = { Text("삭제", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = {
                            Icon(Icons.Default.Delete, null,
                                tint = MaterialTheme.colorScheme.error)
                        },
                        onClick = { menuOpen = false; showDeleteDialog = true },
                    )
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("알람 삭제") },
            text = { Text("\"${alarm.title}\" 알람을 삭제할까요?") },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteDialog = false }) {
                    Text("삭제", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("취소") }
            },
        )
    }
}

private fun shortCountdown(deltaMs: Long): String {
    val mins = (deltaMs / 60_000L).coerceAtLeast(1)
    val days = mins / (60 * 24)
    val hours = (mins / 60) % 24
    val rem = mins % 60
    return when {
        days > 0 -> "${days}일 ${hours}시간 후"
        hours > 0 -> "${hours}시간 ${rem}분 후"
        else -> "${rem}분 후"
    }
}
