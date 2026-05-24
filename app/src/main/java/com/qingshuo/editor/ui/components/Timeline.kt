package com.qingshuo.editor.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.qingshuo.editor.data.Clip
import com.qingshuo.editor.data.Project

@Composable
fun Timeline(
    project: Project,
    selectedClipId: String?,
    currentPlaybackMs: Long,
    onClipSelected: (String) -> Unit,
    onScrubTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val totalMs = project.totalDurationMs.coerceAtLeast(1)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
            .background(MaterialTheme.colorScheme.surface)
            .pointerInput(totalMs) {
                detectHorizontalDragGestures { change, _ ->
                    val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                    onScrubTo((fraction * totalMs).toLong())
                }
            }
    ) {
        if (project.clips.isEmpty()) {
            Text(
                text = "Tap + Add Clip to begin",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp).align(Alignment.Center)
            )
        } else {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                items(project.clips, key = { it.id }) { clip ->
                    ClipBlock(
                        clip = clip,
                        widthFraction = clip.durationMs.toFloat() / totalMs,
                        isSelected = clip.id == selectedClipId,
                        onClick = { onClipSelected(clip.id) }
                    )
                    Spacer(Modifier.width(4.dp))
                }
            }
        }
    }
}

@Composable
private fun ClipBlock(
    clip: Clip,
    widthFraction: Float,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val widthDp = (widthFraction * 600).dp.coerceAtLeast(60.dp)
    Box(
        modifier = Modifier
            .width(widthDp)
            .fillMaxHeight()
            .clip(RoundedCornerShape(6.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = Color.White.copy(alpha = if (isSelected) 0.7f else 0f),
                shape = RoundedCornerShape(6.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = (if (clip.isImage) "IMG " else "") + formatMs(clip.durationMs),
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatMs(ms: Long): String {
    val totalSec = ms / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}
