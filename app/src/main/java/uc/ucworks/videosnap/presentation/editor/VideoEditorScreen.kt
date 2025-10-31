package uc.ucworks.videosnap.presentation.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import uc.ucworks.videosnap.presentation.editor.VideoEditorViewModel
import uc.ucworks.videosnap.presentation.editor.VideoEditorUiState
import uc.ucworks.videosnap.domain.*

/**
 * The main video editor screen with timeline and effects editing.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoEditorScreen(
    projectId: String = "",
    onBackPress: () -> Unit = {}
) {
    val context = LocalContext.current
    val viewModel: VideoEditorViewModel = viewModel {
        VideoEditorViewModel(VideoProcessingEngine(context))
    }
    val uiState by viewModel.uiState.collectAsState()
    val project = uiState.project

    var showExportDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            val videoTrack = project?.tracks?.find { it.type == TrackType.VIDEO }
            if (videoTrack != null) {
                val newClip = TimelineClip(
                    mediaPath = it.toString(),
                    startTime = 0L,
                    endTime = 5000L,
                    mediaType = MediaType.VIDEO
                )
                viewModel.addClipToTrack(videoTrack.id, newClip)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            EditorTopBar(
                projectName = project?.name ?: "Untitled Project",
                onBackPress = onBackPress,
                onImportMedia = { videoPickerLauncher.launch("video/*") },
                onExport = { showExportDialog = true }
            )

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                // Main editing area
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    if (project != null) {
                        // Video Preview
                        VideoPreviewArea(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(300.dp)
                                .padding(8.dp)
                        )

                        Divider(color = Color(0xFF333333))

                        // Timeline Section
                        TimelineSection(
                            project = project,
                            uiState = uiState,
                            viewModel = viewModel,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )

                        Divider(color = Color(0xFF333333))

                        // Bottom Controls
                        EditorBottomControls(
                            zoomLevel = uiState.zoomLevel,
                            onZoomChanged = { viewModel.setZoomLevel(it) },
                            onPlayPause = { viewModel.togglePlayback() },
                            isPlaying = uiState.isPlaying,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Default.VideoLibrary,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = Color.White.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "No project loaded",
                                    color = Color.White.copy(alpha = 0.7f),
                                    fontSize = 16.sp
                                )
                            }
                        }
                    }
                }

                // Right Panel - Effects/Filters
                if (project != null) {
                    EditorRightPanel(
                        selectedTab = selectedTab,
                        onTabSelected = { selectedTab = it },
                        project = project,
                        viewModel = viewModel,
                        modifier = Modifier
                            .width(280.dp)
                            .fillMaxHeight()
                    )
                }
            }
        }
    }

    // Export Dialog
    if (showExportDialog) {
        ExportOptionsDialog(
            onDismiss = { showExportDialog = false },
            onExport = { /* Handle export */ showExportDialog = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorTopBar(
    projectName: String,
    onBackPress: () -> Unit,
    onImportMedia: () -> Unit,
    onExport: () -> Unit,
    modifier: Modifier = Modifier
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = projectName,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        },
        navigationIcon = {
            IconButton(onClick = onBackPress) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
        },
        actions = {
            IconButton(onClick = onImportMedia) {
                Icon(Icons.Default.Add, contentDescription = "Add Media", tint = Color.White)
            }
            IconButton(onClick = onExport) {
                Icon(Icons.Default.FileDownload, contentDescription = "Export", tint = Color(0xFFFFA500))
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF1A1A1A)
        )
    )
}

@Composable
fun VideoPreviewArea(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A))
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    color = Color.Black,
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    Icons.Default.PlayCircle,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFFFFA500).copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Editing Timeline",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun TimelineSection(
    project: VideoProject,
    uiState: VideoEditorUiState,
    viewModel: VideoEditorViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(8.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Timeline",
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Clips List
        if (project.tracks.isNotEmpty()) {
            project.tracks.forEach { track ->
                TimelineTrackItem(
                    track = track,
                    isSelected = false,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Tap '+' to add media to timeline",
                    color = Color.White.copy(alpha = 0.5f),
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun TimelineTrackItem(
    track: TimelineTrack,
    isSelected: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(60.dp)
            .clickable { /* Select track */ },
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF8B5CF6) else Color(0xFF2A2A2A)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (track.type == TrackType.VIDEO) Icons.Default.VideoLibrary else Icons.Default.VolumeUp,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = Color(0xFFFFA500)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = "${track.clips.size} clips",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
fun EditorBottomControls(
    zoomLevel: Float,
    onZoomChanged: (Float) -> Unit,
    onPlayPause: () -> Unit,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(Color(0xFF1A1A1A))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // Play/Pause
        IconButton(onClick = onPlayPause, modifier = Modifier.size(40.dp)) {
            Icon(
                if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = Color(0xFFFFA500),
                modifier = Modifier.size(24.dp)
            )
        }

        // Zoom Control
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.ZoomOut, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
            Slider(
                value = zoomLevel,
                onValueChange = onZoomChanged,
                valueRange = 0.1f..3.0f,
                modifier = Modifier
                    .width(100.dp)
                    .padding(horizontal = 8.dp)
            )
            Icon(Icons.Default.ZoomIn, contentDescription = null, tint = Color.White.copy(alpha = 0.6f))
        }

        Text(
            "${(zoomLevel * 100).toInt()}%",
            fontSize = 11.sp,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.width(35.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorRightPanel(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    project: VideoProject,
    viewModel: VideoEditorViewModel,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color(0xFF1A1A1A))
            .fillMaxHeight()
    ) {
        // Tab Bar
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0F0F0F)),
            containerColor = Color(0xFF1A1A1A)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { onTabSelected(0) },
                text = { Text("Effects", fontSize = 11.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { onTabSelected(1) },
                text = { Text("Filters", fontSize = 11.sp) }
            )
        }

        // Tab Content
        when (selectedTab) {
            0 -> EffectsTab(modifier = Modifier.fillMaxSize())
            1 -> FiltersTab(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
fun EffectsTab(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Effects",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        listOf("Brightness", "Contrast", "Saturation", "Grayscale", "Sepia", "Blur").forEach { effect ->
            EffectButton(effect)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun FiltersTab(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .padding(12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            "Filters",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        listOf("Vintage", "Cinematic", "Warm", "Cool", "High Contrast").forEach { filter ->
            FilterButton(filter)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun EffectButton(name: String, modifier: Modifier = Modifier) {
    Button(
        onClick = { /* Apply effect */ },
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A2A2A)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(name, fontSize = 12.sp, color = Color.White)
    }
}

@Composable
fun FilterButton(name: String, modifier: Modifier = Modifier) {
    Button(
        onClick = { /* Apply filter */ },
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8B5CF6)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(name, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ExportOptionsDialog(
    onDismiss: () -> Unit,
    onExport: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Your Reel") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ExportOption("TikTok", "📱 Share on TikTok", onClick = { onExport("tiktok") })
                ExportOption("YouTube", "▶️ Upload to YouTube", onClick = { onExport("youtube") })
                ExportOption("Instagram", "📸 Post to Instagram", onClick = { onExport("instagram") })
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ExportOption(platform: String, description: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(platform, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
            Text(description, color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
        }
    }
}
