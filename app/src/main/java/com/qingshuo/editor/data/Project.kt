package com.qingshuo.editor.data

import android.net.Uri

/**
 * The user's edit. Holds the ordered clip list, text overlays, and an optional
 * background music track with fade in/out.
 */
data class Project(
    val name: String = "Untitled",
    val clips: List<Clip> = emptyList(),
    val textOverlays: List<TextOverlay> = emptyList(),
    val musicUri: Uri? = null,
    val musicVolume: Float = 0.6f,
    val musicFadeInMs: Long = 1000L,
    val musicFadeOutMs: Long = 1500L,
    val videoVolume: Float = 1.0f
) {
    val totalDurationMs: Long
        get() = clips.sumOf { it.durationMs }

    fun clipStartMs(clipIndex: Int): Long {
        var t = 0L
        for (i in 0 until clipIndex) t += clips[i].durationMs
        return t
    }
}
