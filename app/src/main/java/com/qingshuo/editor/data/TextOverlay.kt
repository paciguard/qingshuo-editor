package com.qingshuo.editor.data

/**
 * Text overlay on the timeline.
 *
 * @param text The text to display.
 * @param startMs Absolute timeline position (ms) where the overlay appears.
 * @param endMs Absolute timeline position (ms) where the overlay disappears.
 * @param xFraction 0..1, horizontal anchor on the video frame.
 * @param yFraction 0..1, vertical anchor on the video frame.
 */
data class TextOverlay(
    val id: String,
    val text: String,
    val startMs: Long,
    val endMs: Long,
    val xFraction: Float = 0.5f,
    val yFraction: Float = 0.85f,
    val sizeSp: Int = 32,
    val argbColor: Int = 0xFFFFFFFF.toInt()
)
