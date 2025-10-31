package uc.ucworks.videosnap

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import uc.ucworks.videosnap.domain.VideoProcessingEngine
import uc.ucworks.videosnap.presentation.VideoEditorViewModel
import uc.ucworks.videosnap.ui.effects.EffectsPanel
import uc.ucworks.videosnap.ui.preview.VideoPreview
import uc.ucworks.videosnap.ui.timeline.TimelineView

/**
 * The main video editor screen with comprehensive editing features.
 * Integrates timeline, preview, effects, and export functionality.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoEditorScreen(
    viewModel: VideoEditorViewModel = viewModel {
        VideoEditorViewModel(VideoProcessingEngine(LocalContext.current))
    }
) {
    val uiState by viewModel.uiState.collectAsState()
    val project = uiState.project
    var showExportDialog by remember { mutableStateOf(false) }
    var showMediaImport by remember { mutableStateOf(false) }
    var showProjectSettings by remember { mutableStateOf(false) }

    // Video picker launcher
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            // Add video to the first video track
            val videoTrack = project?.tracks?.find { it.type == TrackType.VIDEO }
            if (videoTrack != null) {
                val newClip = TimelineClip(
                    mediaPath = it.toString(),
                    startTime = 0L,
                    endTime = 5000L, // Default 5 seconds, will be updated with actual duration
                    mediaType = MediaType.VIDEO
                )
                viewModel.addClipToTrack(videoTrack.id, newClip)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(project?.name ?: "Untitled Project") },
                actions = {
                    IconButton(onClick = { showMediaImport = true }) {
                        Icon(Icons.Default.AddPhotoAlternate, "Import media")
                    }
                    IconButton(onClick = { showProjectSettings = true }) {
                        Icon(Icons.Default.Settings, "Project settings")
                    }
                    IconButton(onClick = { showExportDialog = true }) {
                        Icon(Icons.Default.FileDownload, "Export")
                    }
                }
            )
        }
    ) { padding ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Main editor area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                // Video preview (60% of height)
                VideoPreview(
                    videoPath = project?.tracks
                        ?.flatMap { it.clips }
                        ?.firstOrNull { it.mediaType == MediaType.VIDEO }
                        ?.mediaPath,
                    isPlaying = uiState.isPlaying,
                    currentPosition = uiState.currentPosition,
                    onPlayPauseToggle = { viewModel.togglePlayback() },
                    onSeek = { viewModel.seekTo(it) },
                    modifier = Modifier
                        .weight(0.6f)
                        .fillMaxWidth()
                )

                Divider()

                // Timeline (40% of height)
                Box(modifier = Modifier.weight(0.4f)) {
                    if (project != null) {
                        TimelineView(
                            tracks = project.tracks,
                            currentPosition = uiState.currentPosition,
                            zoomLevel = uiState.zoomLevel,
                            onSeek = { viewModel.seekTo(it) },
                            onClipMoved = { trackId, clipId, newStartTime ->
                                viewModel.updateClip(trackId, clipId) { clip ->
                                    val duration = clip.duration
                                    clip.copy(
                                        startTime = newStartTime,
                                        endTime = newStartTime + duration
                                    )
                                }
                            },
                            onClipSelected = { trackId, clipId ->
                                viewModel.selectClip(trackId, clipId)
                            },
                            onClipResized = { trackId, clipId, newStart, newEnd ->
                                viewModel.updateClip(trackId, clipId) { clip ->
                                    clip.copy(startTime = newStart, endTime = newEnd)
                                }
                            },
                            onTrackAdded = { type ->
                                viewModel.addTrack(type)
                            },
                            selectedClipId = uiState.selectedClipId,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        EmptyProjectMessage(
                            onCreateProject = { viewModel.createNewProject("My Project") }
                        )
                    }
                }

                // Timeline controls
                TimelineControls(
                    zoomLevel = uiState.zoomLevel,
                    onZoomChanged = { viewModel.setZoomLevel(it) },
                    onImportMedia = { videoPickerLauncher.launch("video/*") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                )
            }

            Divider(modifier = Modifier.fillMaxHeight().width(1.dp))

            // Effects panel (30% of width)
            EffectsPanel(
                selectedClipId = uiState.selectedClipId,
                appliedEffects = project?.tracks
                    ?.flatMap { it.clips }
                    ?.find { it.id == uiState.selectedClipId }
                    ?.effects ?: emptyList(),
                onEffectApply = { effectName ->
                    val trackId = uiState.selectedClipTrackId
                    val clipId = uiState.selectedClipId
                    if (trackId != null && clipId != null) {
                        viewModel.applyEffectToClip(trackId, clipId, effectName)
                    }
                },
                onEffectRemove = { effectName ->
                    val trackId = uiState.selectedClipTrackId
                    val clipId = uiState.selectedClipId
                    if (trackId != null && clipId != null) {
                        viewModel.removeEffectFromClip(trackId, clipId, effectName)
                    }
                },
                onTransitionApply = { transition ->
                    val trackId = uiState.selectedClipTrackId
                    val clipId = uiState.selectedClipId
                    if (trackId != null && clipId != null) {
                        viewModel.updateClip(trackId, clipId) { clip ->
                            clip.copy(
                                transitions = clip.transitions + TransitionEffect(
                                    type = transition,
                                    duration = 500L,
                                    position = TransitionPosition.START
                                )
                            )
                        }
                    }
                },
                modifier = Modifier
                    .width(300.dp)
                    .fillMaxHeight()
            )
        }
    }

    // Export dialog
    if (showExportDialog && project != null) {
        ExportDialog(
            project = project,
            onDismiss = { showExportDialog = false },
            onExport = { preset ->
                // TODO: Trigger export with selected preset
                showExportDialog = false
            }
        )
    }

    // Project settings dialog
    if (showProjectSettings && project != null) {
        ProjectSettingsDialog(
            project = project,
            onDismiss = { showProjectSettings = false },
            onSave = { updatedProject ->
                viewModel.loadProject(updatedProject)
                showProjectSettings = false
            }
        )
    }
}

/**
 * Timeline controls (zoom, import, etc.).
 */
@Composable
fun TimelineControls(
    zoomLevel: Float,
    onZoomChanged: (Float) -> Unit,
    onImportMedia: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(onClick = onImportMedia) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Import Media")
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Default.ZoomOut, "Zoom out")
            Slider(
                value = zoomLevel,
                onValueChange = onZoomChanged,
                valueRange = 0.1f..5.0f,
                modifier = Modifier.width(150.dp)
            )
            Icon(Icons.Default.ZoomIn, "Zoom in")
            Text(
                text = "${(zoomLevel * 100).toInt()}%",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.width(45.dp)
            )
        }
    }
}

/**
 * Empty project message.
 */
@Composable
fun EmptyProjectMessage(
    onCreateProject: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.VideoLibrary,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "No project loaded",
                style = MaterialTheme.typography.headlineSmall
            )
            Button(onClick = onCreateProject) {
                Text("Create New Project")
            }
        }
    }
}

/**
 * Export dialog.
 */
@Composable
fun ExportDialog(
    project: VideoProject,
    onDismiss: () -> Unit,
    onExport: (ExportPreset) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Project") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Choose export preset:", style = MaterialTheme.typography.bodyLarge)
                // TODO: Show export presets
                Text("Feature coming soon!")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

/**
 * Project settings dialog.
 */
@Composable
fun ProjectSettingsDialog(
    project: VideoProject,
    onDismiss: () -> Unit,
    onSave: (VideoProject) -> Unit
) {
    var projectName by remember { mutableStateOf(project.name) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Project Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = projectName,
                    onValueChange = { projectName = it },
                    label = { Text("Project Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                // TODO: Add more project settings
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(project.copy(name = projectName))
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
