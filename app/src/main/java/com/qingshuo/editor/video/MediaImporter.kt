package com.qingshuo.editor.video

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.media.MediaMuxer
import android.net.Uri
import com.qingshuo.editor.data.Clip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteBuffer
import java.util.UUID

object MediaImporter {

    private const val IMAGE_DEFAULT_DURATION_MS = 3000L

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
                endMs = duration,
                isImage = false
            )
        } finally {
            try { retriever.release() } catch (_: Exception) {}
        }
    }

    suspend fun importImage(uri: Uri): Clip = withContext(Dispatchers.IO) {
        Clip(
            id = UUID.randomUUID().toString(),
            uri = uri,
            sourceDurationMs = IMAGE_DEFAULT_DURATION_MS,
            startMs = 0,
            endMs = IMAGE_DEFAULT_DURATION_MS,
            isImage = true
        )
    }

    suspend fun extractAudio(context: Context, sourceUri: Uri): Uri? = withContext(Dispatchers.IO) {
        val cacheDir = File(context.cacheDir, "extracted-audio").apply { mkdirs() }
        val outFile = File(cacheDir, "audio-${System.currentTimeMillis()}.m4a")
        val extractor = MediaExtractor()
        var muxer: MediaMuxer? = null
        try {
            context.contentResolver.openFileDescriptor(sourceUri, "r")?.use { pfd ->
                extractor.setDataSource(pfd.fileDescriptor)
            } ?: return@withContext null

            var audioTrack = -1
            for (i in 0 until extractor.trackCount) {
                val fmt = extractor.getTrackFormat(i)
                val mime = fmt.getString(MediaFormat.KEY_MIME) ?: continue
                if (mime.startsWith("audio/")) { audioTrack = i; break }
            }
            if (audioTrack < 0) return@withContext null

            extractor.selectTrack(audioTrack)
            val format = extractor.getTrackFormat(audioTrack)
            muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            val outTrack = muxer.addTrack(format)
            muxer.start()

            val buf = ByteBuffer.allocate(256 * 1024)
            val info = androidx.media.MediaCodec.BufferInfo()
            while (true) {
                buf.clear()
                val size = extractor.readSampleData(buf, 0)
                if (size < 0) break
               info.offset = 0
                info.size = size
                info.presentationTimeUs = extractor.sampleTime
                info.flags = extractor.sampleFlags
                muxer.writeSampleData(outTrack, buf, info)
                extractor.advance()
            }
            Uri.fromFile(outFile)
        } catch (_: Exception) {
            null
        } finally {
            try { extractor.release() } catch (_: Exception) {}
            try { muxer?.stop(); muxer?.release() } catch (_: Exception) {}
        }
    }
}
