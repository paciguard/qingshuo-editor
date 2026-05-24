package com.qingshuo.editor.video

import android.content.Context
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.TypefaceSpan
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.OverlaySettings
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.OverlayEffect
import androidx.media3.effect.RgbFilter
import androidx.media3.effect.TextOverlay as MediaTextOverlay
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.google.common.collect.ImmutableList
import com.qingshuo.editor.data.ClipFilter
import com.qingshuo.editor.data.Project
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Builds a Media3 [Composition] from a [Project] and exports it to an MP4 file.
 *
 * Returns the path to the exported file via the suspend function. Cancellation
 * cancels the in-flight Transformer.
 */
@OptIn(UnstableApi::class)
object VideoExporter {

    suspend fun export(
        context: Context,
        project: Project,
        onProgress: (Float) -> Unit = {}
    ): String = suspendCancellableCoroutine { cont ->

        val exportDir = File(context.getExternalFilesDir(null), "exports").apply { mkdirs() }
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val output = File(exportDir, "qingshuo-$stamp.mp4")

        val editedItems = project.clips.map { clip ->
            val mediaItem = MediaItem.Builder()
                .setUri(clip.uri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(clip.startMs)
                        .setEndPositionMs(clip.endMs)
                        .build()
                )
                .build()

            val effectsList = mutableListOf<androidx.media3.common.Effect>()

            // Color filter
            when (clip.filter) {
                ClipFilter.GRAYSCALE -> effectsList += RgbFilter.createGrayscaleFilter()
                ClipFilter.SEPIA,
                ClipFilter.VINTAGE,
                ClipFilter.COOL,
                ClipFilter.WARM -> {
                    // Lightweight matrix filters omitted in v0.1 — placeholder for future work.
                }
                ClipFilter.NONE -> { /* no-op */ }
            }

            // Text overlays that fall inside this clip's absolute window
            val absStart = project.clips.takeWhile { it !== clip }.sumOf { it.durationMs }
            val absEnd = absStart + clip.durationMs
            val overlaysForClip = project.textOverlays.filter {
                it.startMs < absEnd && it.endMs > absStart
            }

            if (overlaysForClip.isNotEmpty()) {
                val overlays = ImmutableList.copyOf(
                    overlaysForClip.map { t ->
                        val spannable = SpannableString(t.text).apply {
                            setSpan(
                                ForegroundColorSpan(t.argbColor),
                                0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                            setSpan(
                                AbsoluteSizeSpan(t.sizeSp, true),
                                0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                            setSpan(
                                TypefaceSpan("sans-serif"),
                                0, length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                        }
                        TextOverlayImpl(spannable, t.xFraction, t.yFraction)
                    }
                )
                effectsList += OverlayEffect(overlays)
            }

            val builder = EditedMediaItem.Builder(mediaItem)
            if (effectsList.isNotEmpty()) {
                builder.setEffects(Effects(emptyList(), effectsList))
            }
            builder.build()
        }

        if (editedItems.isEmpty()) {
            cont.resumeWithException(IllegalStateException("Project has no clips."))
            return@suspendCancellableCoroutine
        }

        val sequence = EditedMediaItemSequence(editedItems, /* isLooping = */ false)
        val composition = Composition.Builder(listOf(sequence))
            .setEffects(Effects(emptyList(), emptyList()))
            .build()

        val transformer = Transformer.Builder(context)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(comp: Composition, result: ExportResult) {
                    if (cont.isActive) cont.resume(output.absolutePath)
                }

                override fun onError(
                    comp: Composition,
                    result: ExportResult,
                    exception: ExportException
                ) {
                    if (cont.isActive) cont.resumeWithException(exception)
                }
            })
            .build()

        cont.invokeOnCancellation {
            try {
                transformer.cancel()
            } catch (_: Throwable) {
            }
        }

        transformer.start(composition, output.absolutePath)
        // Progress polling (Transformer doesn't push progress; we poll briefly)
        Thread {
            val holder = androidx.media3.transformer.ProgressHolder()
            while (cont.isActive && !output.exists()) {
                try {
                    Thread.sleep(200)
                    val state = transformer.getProgress(holder)
                    if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                        onProgress(holder.progress / 100f)
                    }
                } catch (_: InterruptedException) {
                    break
                }
            }
        }.start()
    }

    /** Adapter from our data class to Media3's TextOverlay abstract class. */
    @UnstableApi
    private class TextOverlayImpl(
        private val spannable: SpannableString,
        private val xFrac: Float,
        private val yFrac: Float
    ) : MediaTextOverlay() {

        private val overlaySettings = OverlaySettings.Builder()
            // Media3 overlay coords are -1..1 with origin at center.
            .setOverlayFrameAnchor((xFrac * 2f - 1f), (1f - yFrac * 2f))
            .build()

        override fun getText(presentationTimeUs: Long) = spannable
        override fun getOverlaySettings() = overlaySettings
    }
}
