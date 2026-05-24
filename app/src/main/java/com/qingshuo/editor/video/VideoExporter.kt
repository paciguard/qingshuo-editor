package com.qingshuo.editor.video

import android.content.Context
import android.os.Handler
import android.os.Looper
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

            val effects = mutableListOf<androidx.media3.common.Effect>()
            if (clip.filter == ClipFilter.GRAYSCALE) {
                effects += RgbFilter.createGrayscaleFilter()
            }
            val builder = EditedMediaItem.Builder(mediaItem)
            if (effects.isNotEmpty()) {
                builder.setEffects(Effects(emptyList(), effects))
            }
            builder.build()
        }

        if (editedItems.isEmpty()) {
            cont.resumeWithException(IllegalStateException("Project has no clips."))
            return@suspendCancellableCoroutine
        }

        val mainHandler = Handler(Looper.getMainLooper())
        mainHandler.post {
            try {
                val sequence = EditedMediaItemSequence(editedItems, false)
                val composition = Composition.Builder(listOf(sequence))
                    .setEffects(Effects(emptyList(), emptyList()))
                    .build()

                val transformer = Transformer.Builder(context)
                    .addListener(object : Transformer.Listener {
                        override fun onCompleted(comp: Composition, result: ExportResult) {
                            if (cont.isActive) cont.resume(output.absolutePath)
                        }
                        override fun onError(comp: Composition, result: ExportResult, exception: ExportException) {
                            if (cont.isActive) cont.resumeWithException(exception)
                        }
                    })
                    .build()

                cont.invokeOnCancellation {
                    mainHandler.post { try { transformer.cancel() } catch (_: Throwable) {} }
                }

                transformer.start(composition, output.absolutePath)

                val holder = androidx.media3.transformer.ProgressHolder()
                val pollRunnable = object : Runnable {
                    override fun run() {
                        if (!cont.isActive) return
                        try {
                            val state = transformer.getProgress(holder)
                            if (state == Transformer.PROGRESS_STATE_AVAILABLE) {
                                onProgress(holder.progress / 100f)
                            }
                            mainHandler.postDelayed(this, 250)
                        } catch (_: Throwable) { }
                    }
                }
                mainHandler.postDelayed(pollRunnable, 250)
            } catch (t: Throwable) {
                if (cont.isActive) cont.resumeWithException(t)
            }
        }
    }
}
