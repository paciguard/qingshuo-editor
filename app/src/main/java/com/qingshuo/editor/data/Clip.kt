package com.qingshuo.editor.data

import android.net.Uri

/**
 * A single clip on the timeline. Can be a video or a still image.
 */
data class Clip(
    val id: String,
    val uri: Uri,
    val sourceDurationMs: Long,
    val startMs: Long = 0,
    val endMs: Long = sourceDurationMs,
    val filter: ClipFilter = ClipFilter.NONE,
    val transitionToNextMs: Long = 0L,
    val isImage: Boolean = false
) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(0)
}

enum class ClipFilter(val displayName: String) {
    NONE("None"),
    GRAYSCALE("B&W"),
    BRIGHT("Bright"),
    DARK("Dark"),
    WARM("Warm"),
    COOL("Cool"),
    VIVID("Vivid"),
    FADED("Faded")
}
