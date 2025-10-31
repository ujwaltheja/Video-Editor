package uc.ucworks.videosnap.presentation.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import uc.ucworks.videosnap.domain.*

data class VideoEditorUiState(
    val project: VideoProject? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val zoomLevel: Float = 1.0f,
    val selectedClipId: String? = null,
    val selectedClipTrackId: String? = null,
)

class VideoEditorViewModel(
    private val videoProcessingEngine: VideoProcessingEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(VideoEditorUiState())
    val uiState: StateFlow<VideoEditorUiState> = _uiState.asStateFlow()

    fun createNewProject(name: String) {
        val newProject = VideoProject(name = name)
        _uiState.update { it.copy(project = newProject) }
    }

    fun loadProject(project: VideoProject) {
        _uiState.update { it.copy(project = project) }
    }

    fun addTrack(type: TrackType) {
        val newTrack = TimelineTrack(type = type)
        _uiState.update { currentState ->
            val updatedTracks = currentState.project?.tracks.orEmpty() + newTrack
            currentState.copy(project = currentState.project?.copy(tracks = updatedTracks))
        }
    }

    fun addClipToTrack(trackId: String, clip: TimelineClip) {
        _uiState.update { currentState ->
            val updatedTracks = currentState.project?.tracks.orEmpty().map {
                if (it.id == trackId) {
                    it.copy(clips = it.clips + clip)
                } else {
                    it
                }
            }
            currentState.copy(project = currentState.project?.copy(tracks = updatedTracks))
        }
    }

    fun updateClip(trackId: String, clipId: String, updateAction: (TimelineClip) -> TimelineClip) {
        _uiState.update { currentState ->
            val updatedTracks = currentState.project?.tracks.orEmpty().map {
                if (it.id == trackId) {
                    it.copy(clips = it.clips.map {
                        if (it.id == clipId) {
                            updateAction(it)
                        } else {
                            it
                        }
                    })
                } else {
                    it
                }
            }
            currentState.copy(project = currentState.project?.copy(tracks = updatedTracks))
        }
    }

    fun selectClip(trackId: String, clipId: String) {
        _uiState.update { it.copy(selectedClipId = clipId, selectedClipTrackId = trackId) }
    }

    fun togglePlayback() {
        _uiState.update { it.copy(isPlaying = !it.isPlaying) }
    }

    fun seekTo(position: Long) {
        _uiState.update { it.copy(currentPosition = position) }
    }

    fun setZoomLevel(zoom: Float) {
        _uiState.update { it.copy(zoomLevel = zoom) }
    }

    fun applyEffectToClip(trackId: String, clipId: String, effectName: String) {
        val newEffect = Effect(name = effectName)
        updateClip(trackId, clipId) { clip ->
            clip.copy(effects = clip.effects + newEffect)
        }
    }

    fun removeEffectFromClip(trackId: String, clipId: String, effectName: String) {
        updateClip(trackId, clipId) { clip ->
            clip.copy(effects = clip.effects.filter { it.name != effectName })
        }
    }
}
