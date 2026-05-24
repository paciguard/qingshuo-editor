package com.qingshuo.editor.ui.components

import android.view.ViewGroup
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.qingshuo.editor.data.Project

@OptIn(UnstableApi::class)
@Composable
fun VideoPreview(
    project: Project,
    isPlaying: Boolean,
    scrubToMs: Long?,
    onPositionUpdate: (Long) -> Unit,
    onScrubHandled: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Only video clips are fed to ExoPlayer; image clips are rendered as Compose AsyncImage.
    val videoClips = project.clips.filter { !it.isImage }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = false
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    LaunchedEffect(videoClips.map { Triple(it.id, it.uri.toString(), it.durationMs) }) {
        exoPlayer.clearMediaItems()
        videoClips.forEach { clip ->
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

    // Track which clip is currently "active" based on the timeline playback position.
    // For image clips we use a coroutine to advance playbackMs locally.
    var localPlaybackMs by remember { mutableStateOf(0L) }

    fun activeClipIndex(absMs: Long): Int {
        var acc = 0L
        for ((i, c) in project.clips.withIndex()) {
            if (absMs < acc + c.durationMs) return i
            acc += c.durationMs
        }
        return (project.clips.size - 1).coerceAtLeast(0)
    }

    LaunchedEffect(isPlaying, project.clips.size) {
        exoPlayer.playWhenReady = isPlaying
        // While "playing" an image clip, advance localPlaybackMs ourselves.
        while (isPlaying && project.clips.isNotEmpty()) {
            kotlinx.coroutines.delay(33)
            val idx = activeClipIndex(localPlaybackMs)
            if (idx in project.clips.indices && project.clips[idx].isImage) {
                localPlaybackMs += 33
            } else {
                // Video clip — sync from ExoPlayer
                val abs = project.clipStartMs(exoPlayer.currentMediaItemIndex.coerceAtLeast(0))
                localPlaybackMs = abs + exoPlayer.currentPosition.coerceAtLeast(0)
            }
            onPositionUpdate(localPlaybackMs)
        }
    }

    LaunchedEffect(scrubToMs) {
        val target = scrubToMs ?: return@LaunchedEffect
        localPlaybackMs = target
        // Map to video clip index for ExoPlayer
        var abs = 0L
        var vIdx = 0
        var local = 0L
        for ((i, c) in videoClips.withIndex()) {
            if (target < abs + c.durationMs) { vIdx = i; local = target - abs; break }
            abs += c.durationMs
        }
        if (videoClips.isNotEmpty()) {
            exoPlayer.seekTo(vIdx, local + videoClips[vIdx].startMs)
        }
        onScrubHandled()
    }

    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    val activeIdx = activeClipIndex(localPlaybackMs)
    val activeClip = project.clips.getOrNull(activeIdx)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
    ) {
        // Always show PlayerView underneath; it just stays black for image-only projects.
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
        // Overlay AsyncImage when the active clip is an image.
        if (activeClip != null && activeClip.isImage) {
            AsyncImage(
                model = activeClip.uri,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
