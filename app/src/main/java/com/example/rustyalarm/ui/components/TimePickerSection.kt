package com.example.rustyalarm.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Wheel-style time picker (iOS-like). Two vertically scrollable columns
 * (hour, minute) with snap-to-item. Selected row sits at the centre marker.
 */
@Composable
fun TimePickerSection(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val hours   = remember { (0..23).toList() }
    val minutes = remember { (0..59).toList() }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "%02d:%02d".format(hour, minute),
            fontSize = 56.sp,
            fontWeight = FontWeight.Thin,
            color = MaterialTheme.colorScheme.primary,
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
            contentAlignment = Alignment.Center,
        ) {
            // Selection band — sits behind the picker rows
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    ),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WheelColumn(
                    values = hours,
                    selected = hour,
                    onSelected = onHourChange,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    ":",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
                WheelColumn(
                    values = minutes,
                    selected = minute,
                    onSelected = onMinuteChange,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WheelColumn(
    values: List<Int>,
    selected: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowHeight = 48.dp
    val visibleCount = 3   // rows shown above + selected + below

    val listState: LazyListState = rememberLazyListState(
        initialFirstVisibleItemIndex = selected.coerceIn(0, values.size - 1),
    )
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)

    // Drive the parent state from the snapped row
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { idx ->
                if (!listState.isScrollInProgress) {
                    onSelected(values[idx.coerceIn(0, values.lastIndex)])
                }
            }
    }
    // Also commit when scroll settles
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val idx = listState.firstVisibleItemIndex.coerceIn(0, values.lastIndex)
            onSelected(values[idx])
        }
    }

    Box(
        modifier = modifier.height(rowHeight * visibleCount),
        contentAlignment = Alignment.Center,
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = rowHeight),
        ) {
            itemsIndexed(values) { idx, v ->
                val isSelected = idx == listState.firstVisibleItemIndex
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeight),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "%02d".format(v),
                        textAlign = TextAlign.Center,
                        fontSize = if (isSelected) 30.sp else 22.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    )
                }
            }
        }
    }
}
