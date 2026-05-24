package com.qingshuo.editor.video

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.RgbFilter
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
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
 * Builds a Media3 Composition from a Project and exports it to an MP4 file.
 *
 * v0.1 keeps only essentials (trim, merge, B&W filter) so the APK compiles
 * cleanly against media3 1.2.1. Text overlays / transitions are stubs that
 * the UI accepts but the export ignores; they'll come back in v0.2 once we
 * upgrade media3.
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
            if (clip.filter == ClipFilter.GRAYSCALE) {
                effectsList += RgbFilter.createGrayscaleFilter()
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
            try { transformer.cancel() } catch (_: Throwable) {}
        }

        transformer.start(composition, output.absolutePath)
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
}
