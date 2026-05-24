package com.qingshuo.editor.data

import android.net.Uri

/**
 * A single clip on the timeline.
 *
 * @param uri Content URI of the source media.
 * @param sourceDurationMs Duration of the source file in ms.
 * @param startMs Trim-in point in the source (ms).
 * @param endMs Trim-out point in the source (ms). Must be > startMs.
 * @param filter Optional visual filter applied to the whole clip.
 * @param transitionToNextMs Crossfade duration to the next clip (0 = cut).
 */
data class Clip(
    val id: String,
    val uri: Uri,
    val sourceDurationMs: Long,
    val startMs: Long = 0,
    val endMs: Long = sourceDurationMs,
    val filter: ClipFilter = ClipFilter.NONE,
    val transitionToNextMs: Long = 0L
) {
    val durationMs: Long get() = (endMs - startMs).coerceAtLeast(0)
}

enum class ClipFilter(val displayName: String) {
    NONE("None"),
    GRAYSCALE("B&W"),
    SEPIA("Sepia"),
    VINTAGE("Vintage"),
    COOL("Cool"),
    WARM("Warm")
}
