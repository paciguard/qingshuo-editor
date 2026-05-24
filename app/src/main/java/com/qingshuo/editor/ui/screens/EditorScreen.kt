package com.qingshuo.editor.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.qingshuo.editor.data.ClipFilter
import com.qingshuo.editor.ui.components.AddTextDialog
import com.qingshuo.editor.ui.components.EditorToolBar
import com.qingshuo.editor.ui.components.ExportDoneDialog
import com.qingshuo.editor.ui.components.ExportErrorDialog
import com.qingshuo.editor.ui.components.ExportProgressDialog
import com.qingshuo.editor.ui.components.FilterPickerDialog
import com.qingshuo.editor.ui.components.Timeline
import com.qingshuo.editor.ui.components.TransitionPickerDialog
import com.qingshuo.editor.ui.components.VideoPreview
import com.qingshuo.editor.viewmodel.EditorViewModel

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    onExit: () -> Unit,
    vm: EditorViewModel = viewModel()
) {
    val project by vm.project.collectAsState()
    val selectedId by vm.selectedClipId.collectAsState()
    val exportState by vm.exportState.collectAsState()

    var isPlaying by remember { mutableStateOf(false) }
    var showText by remember { mutableStateOf(false) }
    var showFilter by remember { mutableStateOf(false) }
    var showTransition by remember { mutableStateOf(false) }

    val pickVideo = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { vm.importClip(it) } }

    val pickMusic = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> vm.setMusic(uri) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit", color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onExit) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.export() }) {
                        Icon(Icons.Filled.FileDownload, contentDescription = "Export")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                VideoPreview(
                    project = project,
                    isPlaying = isPlaying,
                    onPositionUpdate = vm::updatePlayback
                )
                IconButton(
                    onClick = { isPlaying = !isPlaying },
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(56.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Timeline(
                project = project,
                selectedClipId = selectedId,
                onClipSelected = vm::setSelectedClip
            )

            Spacer(Modifier.weight(1f))

            EditorToolBar(
                onAddClip = { pickVideo.launch(arrayOf("video/*")) },
                onSplit = vm::splitSelectedAtPlayhead,
                onDelete = vm::deleteSelected,
                onAddText = { showText = true },
                onFilter = { showFilter = true },
                onTransition = { showTransition = true },
                onMusic = { pickMusic.launch(arrayOf("audio/*")) }
            )
        }
    }

    if (showText) {
        AddTextDialog(
            onConfirm = { vm.addTextOverlay(it); showText = false },
            onDismiss = { showText = false }
        )
    }

    if (showFilter) {
        val current = project.clips.firstOrNull { it.id == selectedId }?.filter ?: ClipFilter.NONE
        FilterPickerDialog(
            current = current,
            onPick = vm::setFilterOnSelected,
            onDismiss = { showFilter = false }
        )
    }

    if (showTransition) {
        TransitionPickerDialog(
            onPick = vm::setTransitionOnSelected,
            onDismiss = { showTransition = false }
        )
    }

    when (val s = exportState) {
        is EditorViewModel.ExportState.Running -> ExportProgressDialog(s.progress)
        is EditorViewModel.ExportState.Done -> ExportDoneDialog(s.path, vm::acknowledgeExportResult)
        is EditorViewModel.ExportState.Failed -> ExportErrorDialog(s.message, vm::acknowledgeExportResult)
        EditorViewModel.ExportState.Idle -> { /* no-op */ }
    }
}
