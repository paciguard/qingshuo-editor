package com.qingshuo.editor.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.qingshuo.editor.data.ClipFilter
import com.qingshuo.editor.data.Project
import com.qingshuo.editor.data.TextOverlay
import com.qingshuo.editor.video.MediaImporter
import com.qingshuo.editor.video.VideoExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class EditorViewModel(app: Application) : AndroidViewModel(app) {

    private val _project = MutableStateFlow(Project())
    val project: StateFlow<Project> = _project.asStateFlow()

    private val _selectedClipId = MutableStateFlow<String?>(null)
    val selectedClipId: StateFlow<String?> = _selectedClipId.asStateFlow()

    private val _playbackMs = MutableStateFlow(0L)
    val playbackMs: StateFlow<Long> = _playbackMs.asStateFlow()

    private val _scrubToMs = MutableStateFlow<Long?>(null)
    val scrubToMs: StateFlow<Long?> = _scrubToMs.asStateFlow()

    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    fun importClip(uri: Uri) = viewModelScope.launch {
        val clip = MediaImporter.importVideo(getApplication(), uri)
        _project.update { it.copy(clips = it.clips + clip) }
        if (_selectedClipId.value == null) _selectedClipId.value = clip.id
    }

    fun importImage(uri: Uri) = viewModelScope.launch {
        val clip = MediaImporter.importImage(uri)
        _project.update { it.copy(clips = it.clips + clip) }
        if (_selectedClipId.value == null) _selectedClipId.value = clip.id
    }

    fun importMusicFromVideo(uri: Uri) = viewModelScope.launch {
        val audioUri = MediaImporter.extractAudio(getApplication(), uri)
        if (audioUri != null) {
            _project.update { it.copy(musicUri = audioUri) }
        }
    }

    fun setSelectedClip(id: String?) {
        _selectedClipId.value = id
    }

    fun updatePlayback(ms: Long) {
        _playbackMs.value = ms
    }

    fun requestScrubTo(ms: Long) {
        _scrubToMs.value = ms
        _playbackMs.value = ms
    }

    fun acknowledgeScrub() {
        _scrubToMs.value = null
    }

    fun splitSelectedAtPlayhead() {
        val sel = _selectedClipId.value ?: return
        _project.update { p ->
            val idx = p.clips.indexOfFirst { it.id == sel }
            if (idx < 0) return@update p
            val abs = _playbackMs.value
            val clipStart = p.clipStartMs(idx)
            val target = p.clips[idx]
            val localCut = (abs - clipStart).coerceIn(1, target.durationMs - 1)
            if (localCut <= 0 || localCut >= target.durationMs) return@update p
            val left = target.copy(endMs = target.startMs + localCut)
            val right = target.copy(
                id = UUID.randomUUID().toString(),
                startMs = target.startMs + localCut
            )
            val newClips = p.clips.toMutableList().apply {
                removeAt(idx); add(idx, left); add(idx + 1, right)
            }
            p.copy(clips = newClips)
        }
    }

    fun deleteSelected() {
        val sel = _selectedClipId.value ?: return
        _project.update { p -> p.copy(clips = p.clips.filterNot { it.id == sel }) }
        _selectedClipId.value = _project.value.clips.firstOrNull()?.id
    }

    fun trimSelected(startMs: Long, endMs: Long) {
        val sel = _selectedClipId.value ?: return
        _project.update { p ->
            p.copy(clips = p.clips.map {
                if (it.id == sel) it.copy(startMs = startMs, endMs = endMs) else it
            })
        }
    }

    fun setFilterOnSelected(filter: ClipFilter) {
        val sel = _selectedClipId.value ?: return
        _project.update { p ->
            p.copy(clips = p.clips.map {
                if (it.id == sel) it.copy(filter = filter) else it
            })
        }
    }

    fun setTransitionOnSelected(durationMs: Long) {
        val sel = _selectedClipId.value ?: return
        _project.update { p ->
            p.copy(clips = p.clips.map {
                if (it.id == sel) it.copy(transitionToNextMs = durationMs) else it
            })
        }
    }

    fun addTextOverlay(text: String) {
        val p = _project.value
        val now = _playbackMs.value
        val end = (now + 3000).coerceAtMost(p.totalDurationMs.coerceAtLeast(now + 3000))
        val overlay = TextOverlay(
            id = UUID.randomUUID().toString(),
            text = text,
            startMs = now,
            endMs = end
        )
        _project.update { it.copy(textOverlays = it.textOverlays + overlay) }
    }

    fun setMusic(uri: Uri?) {
        _project.update { it.copy(musicUri = uri) }
    }

    fun setMusicFade(fadeInMs: Long, fadeOutMs: Long) {
        _project.update { it.copy(musicFadeInMs = fadeInMs, musicFadeOutMs = fadeOutMs) }
    }

    fun export() {
        if (_project.value.clips.isEmpty()) {
            _exportState.value = ExportState.Failed("Add at least one clip.")
            return
        }
        _exportState.value = ExportState.Running(0f)
        viewModelScope.launch {
            try {
                val path = VideoExporter.export(
                    getApplication(),
                    _project.value
                ) { progress ->
                    _exportState.value = ExportState.Running(progress)
                }
                _exportState.value = ExportState.Done(path)
            } catch (t: Throwable) {
                _exportState.value = ExportState.Failed(t.message ?: "Unknown error")
            }
        }
    }

    fun acknowledgeExportResult() {
        _exportState.value = ExportState.Idle
    }

    sealed interface ExportState {
        data object Idle : ExportState
        data class Running(val progress: Float) : ExportState
        data class Done(val path: String) : ExportState
        data class Failed(val message: String) : ExportState
    }
}
