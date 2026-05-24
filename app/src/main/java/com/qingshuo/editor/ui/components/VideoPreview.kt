package com.qingshuo.editor.ui.components

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.qingshuo.editor.data.Project

@OptIn(UnstableApi::class)
@Composable
fun VideoPreview(
    project: Project,
    isPlaying: Boolean,
    onPositionUpdate: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = false
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    // Sync the playlist whenever the clip list changes.
    LaunchedEffect(project.clips.map { it.id to it.uri.toString() to (it.startMs to it.endMs) }) {
        exoPlayer.clearMediaItems()
        project.clips.forEach { clip ->
            val item = MediaItem.Builder()
                .setUri(clip.uri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(clip.startMs)
                        .setEndPositionMs(clip.endMs)
                        .build()
                )
                .build()
            exoPlayer.addMediaItem(item)
        }
        exoPlayer.prepare()
    }

    LaunchedEffect(isPlaying) {
        exoPlayer.playWhenReady = isPlaying
    }

    // Position polling
    LaunchedEffect(exoPlayer) {
        while (true) {
            onPositionUpdate(
                project.clipStartMs(exoPlayer.currentMediaItemIndex.coerceAtLeast(0))
                        + exoPlayer.currentPosition.coerceAtLeast(0)
            )
            kotlinx.coroutines.delay(60)
        }
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    player = exoPlayer
                    useController = false
                }
            }
        )
    }
}
