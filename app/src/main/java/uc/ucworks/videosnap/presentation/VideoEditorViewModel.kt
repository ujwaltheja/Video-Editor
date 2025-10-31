package uc.ucworks.videosnap.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uc.ucworks.videosnap.*
import uc.ucworks.videosnap.domain.VideoProcessingEngine

/**
 * ViewModel for the video editor screen.
 * Manages the state of the video project and handles all editing operations.
 */
class VideoEditorViewModel(
    private val processingEngine: VideoProcessingEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoEditorUiState())
    val uiState: StateFlow<VideoEditorUiState> = _uiState.asStateFlow()

    init {
        // Initialize with a default project
        createNewProject("Untitled Project")
    }

    /**
     * Create a new project.
     */
    fun createNewProject(name: String) {
        val newProject = VideoProject.createNew(
            name = name,
            tracks = listOf(
                TimelineTrack(
                    name = "Video Track 1",
                    type = TrackType.VIDEO,
                    order = 0
                ),
                TimelineTrack(
                    name = "Audio Track 1",
                    type = TrackType.AUDIO,
                    order = 1
                )
            )
        )
        _uiState.update { it.copy(project = newProject) }
    }

    /**
     * Load an existing project.
     */
    fun loadProject(project: VideoProject) {
        _uiState.update { it.copy(project = project) }
    }

    /**
     * Add a clip to a track.
     */
    fun addClipToTrack(trackId: String, clip: TimelineClip) {
        val currentProject = _uiState.value.project ?: return
        val updatedTracks = currentProject.tracks.map { track ->
            if (track.id == trackId) {
                track.copy(clips = track.clips + clip)
            } else {
                track
            }
        }
        updateProject(currentProject.copy(
            tracks = updatedTracks,
            lastModified = System.currentTimeMillis()
        ))
    }

    /**
     * Remove a clip from a track.
     */
    fun removeClip(trackId: String, clipId: String) {
        val currentProject = _uiState.value.project ?: return
        val updatedTracks = currentProject.tracks.map { track ->
            if (track.id == trackId) {
                track.copy(clips = track.clips.filter { it.id != clipId })
            } else {
                track
            }
        }
        updateProject(currentProject.copy(
            tracks = updatedTracks,
            lastModified = System.currentTimeMillis()
        ))
    }

    /**
     * Update a clip's properties.
     */
    fun updateClip(trackId: String, clipId: String, updater: (TimelineClip) -> TimelineClip) {
        val currentProject = _uiState.value.project ?: return
        val updatedTracks = currentProject.tracks.map { track ->
            if (track.id == trackId) {
                track.copy(clips = track.clips.map { clip ->
                    if (clip.id == clipId) updater(clip) else clip
                })
            } else {
                track
            }
        }
        updateProject(currentProject.copy(
            tracks = updatedTracks,
            lastModified = System.currentTimeMillis()
        ))
    }

    /**
     * Add a new track.
     */
    fun addTrack(type: TrackType = TrackType.VIDEO) {
        val currentProject = _uiState.value.project ?: return
        val trackCount = currentProject.tracks.count { it.type == type }
        val newTrack = TimelineTrack(
            name = "${type.name} Track ${trackCount + 1}",
            type = type,
            order = currentProject.tracks.size
        )
        updateProject(currentProject.copy(
            tracks = currentProject.tracks + newTrack,
            lastModified = System.currentTimeMillis()
        ))
    }

    /**
     * Remove a track.
     */
    fun removeTrack(trackId: String) {
        val currentProject = _uiState.value.project ?: return
        val updatedTracks = currentProject.tracks.filter { it.id != trackId }
        updateProject(currentProject.copy(
            tracks = updatedTracks,
            lastModified = System.currentTimeMillis()
        ))
    }

    /**
     * Update playback position.
     */
    fun seekTo(positionMs: Long) {
        _uiState.update { it.copy(currentPosition = positionMs) }
        val currentProject = _uiState.value.project ?: return
        updateProject(currentProject.copy(currentPosition = positionMs))
    }

    /**
     * Play/Pause playback.
     */
    fun togglePlayback() {
        _uiState.update { it.copy(isPlaying = !it.isPlaying) }
    }

    /**
     * Update zoom level for timeline.
     */
    fun setZoomLevel(zoom: Float) {
        _uiState.update { it.copy(zoomLevel = zoom.coerceIn(0.1f, 5.0f)) }
    }

    /**
     * Select a clip for editing.
     */
    fun selectClip(trackId: String, clipId: String) {
        _uiState.update {
            it.copy(
                selectedClipTrackId = trackId,
                selectedClipId = clipId
            )
        }
    }

    /**
     * Deselect current clip.
     */
    fun deselectClip() {
        _uiState.update {
            it.copy(
                selectedClipTrackId = null,
                selectedClipId = null
            )
        }
    }

    /**
     * Split a clip at the current playback position.
     */
    fun splitClipAtPosition(trackId: String, clipId: String, positionMs: Long) {
        val currentProject = _uiState.value.project ?: return
        val track = currentProject.tracks.find { it.id == trackId } ?: return
        val clip = track.clips.find { it.id == clipId } ?: return

        if (positionMs <= clip.startTime || positionMs >= clip.endTime) return

        val clip1 = clip.copy(
            id = "${clip.id}_1",
            endTime = positionMs
        )
        val clip2 = clip.copy(
            id = "${clip.id}_2",
            startTime = positionMs,
            trimStart = clip.trimStart + (positionMs - clip.startTime)
        )

        val updatedTracks = currentProject.tracks.map { t ->
            if (t.id == trackId) {
                t.copy(clips = t.clips.flatMap { c ->
                    if (c.id == clipId) listOf(clip1, clip2) else listOf(c)
                })
            } else {
                t
            }
        }

        updateProject(currentProject.copy(
            tracks = updatedTracks,
            lastModified = System.currentTimeMillis()
        ))
    }

    /**
     * Apply an effect to a clip.
     */
    fun applyEffectToClip(trackId: String, clipId: String, effectName: String) {
        updateClip(trackId, clipId) { clip ->
            clip.copy(effects = clip.effects + effectName)
        }
    }

    /**
     * Remove an effect from a clip.
     */
    fun removeEffectFromClip(trackId: String, clipId: String, effectName: String) {
        updateClip(trackId, clipId) { clip ->
            clip.copy(effects = clip.effects - effectName)
        }
    }

    /**
     * Update the entire project.
     */
    private fun updateProject(project: VideoProject) {
        _uiState.update { it.copy(project = project) }
    }

    /**
     * Undo last action.
     */
    fun undo() {
        // TODO: Implement undo/redo with command pattern
    }

    /**
     * Redo last undone action.
     */
    fun redo() {
        // TODO: Implement undo/redo with command pattern
    }
}

/**
 * UI state for the video editor.
 */
data class VideoEditorUiState(
    val project: VideoProject? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val zoomLevel: Float = 1.0f,
    val selectedClipTrackId: String? = null,
    val selectedClipId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
