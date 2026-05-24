package com.qingshuo.editor.video

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.qingshuo.editor.data.Clip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/** Reads basic metadata (duration) from a content URI so we can create a [Clip]. */
object MediaImporter {

    suspend fun importVideo(context: Context, uri: Uri): Clip = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            retriever.setDataSource(context, uri)
            val duration = retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
            Clip(
                id = UUID.randomUUID().toString(),
                uri = uri,
                sourceDurationMs = duration,
                startMs = 0,
                endMs = duration
            )
        } finally {
            try {
                retriever.release()
            } catch (_: Exception) {
            }
        }
    }
}
