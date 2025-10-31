package uc.ucworks.videosnap.presentation.editor

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import uc.ucworks.videosnap.*
import uc.ucworks.videosnap.data.repository.ProjectRepository
import uc.ucworks.videosnap.domain.VideoProject
import uc.ucworks.videosnap.domain.engine.*
import uc.ucworks.videosnap.domain.export.ExportEngine
import uc.ucworks.videosnap.domain.export.ExportPreset
import uc.ucworks.videosnap.domain.export.ExportProgress
import uc.ucworks.videosnap.domain.rendering.RenderingEngine
import javax.inject.Inject

data class EditorUiState(
    val project: VideoProject? = null,
    val isPlaying: Boolean = false,
    val currentPosition: Long = 0L,
    val zoomLevel: Float = 1.0f,
    val selectedClipId: String? = null,
    val selectedClipTrackId: String? = null,
    val previewFrame: Bitmap? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val exportProgress: ExportProgress? = null,
    val availableEffects: List<VideoEffect> = emptyList(),
    val availableTransitions: List<TransitionType> = emptyList(),
    val exportPresets: List<ExportPreset> = emptyList()
)

@HiltViewModel
class VideoEditorViewModelNew @Inject constructor(
    private val repository: ProjectRepository,
    private val renderingEngine: RenderingEngine,
    private val effectsEngine: EffectsEngine,
    private val transitionEngine: TransitionEngine,
    private val audioEngine: AudioEngine,
    private val keyframeEngine: KeyframeEngine,
    private val exportEngine: ExportEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()

    private var autoSaveJob: kotlinx.coroutines.Job? = null

    init {
        loadAvailableResources()
    }

    fun loadProject(projectId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                repository.getProjectById(projectId).collect { project ->
                    _uiState.update {
                        it.copy(
                            project = project,
                            isLoading = false,
                            error = null
                        )
                    }
                    if (project != null) {
                        startAutoSave(project)
                        updatePreview()
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message
                    )
                }
            }
        }
    }

    fun createNewProject(name: String) {
        viewModelScope.launch {
            try {
                val project = repository.createNewProject(name)
                _uiState.update { it.copy(project = project) }
                startAutoSave(project)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message) }
            }
        }
    }

    // Track operations
    fun addTrack(type: TrackType) {
        val project = _uiState.value.project ?: return
        val newTrack = TimelineTrack(
            type = type,
            name = "${type.name} Track ${project.tracks.size + 1}"
        )
        val updated = project.copy(tracks = project.tracks + newTrack)
        updateProject(updated)
    }

    fun removeTrack(trackId: String) {
        val project = _uiState.value.project ?: return
        val updated = project.copy(
            tracks = project.tracks.filter { it.id != trackId }
        )
        updateProject(updated)
    }

    // Clip operations
    fun addClipToTrack(trackId: String, clip: TimelineClip) {
        val project = _uiState.value.project ?: return
        val updated = project.copy(
            tracks = project.tracks.map { track ->
                if (track.id == trackId) {
                    track.copy(clips = track.clips + clip)
                } else {
                    track
                }
            }
        )
        updateProject(updated)
        updatePreview()
    }

    fun trimClip(trackId: String, clipId: String, newStart: Long, newEnd: Long) {
        updateClip(trackId, clipId) { clip ->
            clip.copy(
                trimStart = newStart,
                trimEnd = newEnd
            )
        }
    }

    fun splitClipAt(trackId: String, clipId: String, splitTime: Long) {
        val project = _uiState.value.project ?: return
        val track = project.tracks.find { it.id == trackId } ?: return
        val clip = track.clips.find { it.id == clipId } ?: return

        val firstClip = clip.copy(
            id = "${clip.id}_split1",
            endTime = splitTime
        )
        val secondClip = clip.copy(
            id = "${clip.id}_split2",
            startTime = splitTime,
            offsetX = clip.offsetX + (splitTime - clip.startTime)
        )

        val updated = project.copy(
            tracks = project.tracks.map { t ->
                if (t.id == trackId) {
                    t.copy(
                        clips = t.clips.flatMap { c ->
                            if (c.id == clipId) {
                                listOf(firstClip, secondClip)
                            } else {
                                listOf(c)
                            }
                        }
                    )
                } else {
                    t
                }
            }
        )
        updateProject(updated)
    }

    fun deleteClip(trackId: String, clipId: String) {
        val project = _uiState.value.project ?: return
        val updated = project.copy(
            tracks = project.tracks.map { track ->
                if (track.id == trackId) {
                    track.copy(clips = track.clips.filter { it.id != clipId })
                } else {
                    track
                }
            }
        )
        updateProject(updated)
    }

    fun rippleDelete(trackId: String, clipId: String) {
        val project = _uiState.value.project ?: return
        val track = project.tracks.find { it.id == trackId } ?: return
        val clip = track.clips.find { it.id == clipId } ?: return

        val gapDuration = clip.duration

        val updated = project.copy(
            tracks = project.tracks.map { t ->
                if (t.id == trackId) {
                    t.copy(
                        clips = t.clips.mapNotNull { c ->
                            when {
                                c.id == clipId -> null // Remove the clip
                                c.offsetX > clip.offsetX -> c.copy(offsetX = c.offsetX - gapDuration) // Move clips after
                                else -> c // Keep clips before unchanged
                            }
                        }
                    )
                } else {
                    t
                }
            }
        )
        updateProject(updated)
    }

    // Effect operations
    fun applyEffectToClip(trackId: String, clipId: String, effect: VideoEffect) {
        updateClip(trackId, clipId) { clip ->
            clip.copy(effects = clip.effects + effect.name)
        }
    }

    fun removeEffectFromClip(trackId: String, clipId: String, effectName: String) {
        updateClip(trackId, clipId) { clip ->
            clip.copy(effects = clip.effects.filter { it != effectName })
        }
    }

    // Transition operations
    fun addTransition(trackId: String, clipId: String, transition: TransitionEffect) {
        updateClip(trackId, clipId) { clip ->
            clip.copy(transitions = clip.transitions + transition)
        }
    }

    // Keyframe operations
    fun addKeyframe(trackId: String, clipId: String, keyframe: Keyframe) {
        updateClip(trackId, clipId) { clip ->
            clip.copy(keyframes = keyframeEngine.addKeyframe(clip.keyframes, keyframe))
        }
    }

    fun removeKeyframe(trackId: String, clipId: String, time: Double) {
        updateClip(trackId, clipId) { clip ->
            clip.copy(keyframes = keyframeEngine.removeKeyframe(clip.keyframes, time))
        }
    }

    // Playback controls
    fun togglePlayback() {
        _uiState.update { it.copy(isPlaying = !it.isPlaying) }
        if (_uiState.value.isPlaying) {
            startPlayback()
        }
    }

    fun seekTo(position: Long) {
        _uiState.update { it.copy(currentPosition = position) }
        updatePreview()
    }

    fun setZoomLevel(zoom: Float) {
        _uiState.update { it.copy(zoomLevel = zoom.coerceIn(0.1f, 10f)) }
    }

    fun selectClip(trackId: String, clipId: String) {
        _uiState.update {
            it.copy(
                selectedClipId = clipId,
                selectedClipTrackId = trackId
            )
        }
    }

    // Export
    fun exportProject(preset: ExportPreset, outputPath: String) {
        val project = _uiState.value.project ?: return
        viewModelScope.launch {
            exportEngine.exportProject(project, preset, outputPath).collect { progress ->
                _uiState.update { it.copy(exportProgress = progress) }
            }
        }
    }

    // Private helpers
    private fun updateClip(trackId: String, clipId: String, update: (TimelineClip) -> TimelineClip) {
        val project = _uiState.value.project ?: return
        val updated = project.copy(
            tracks = project.tracks.map { track ->
                if (track.id == trackId) {
                    track.copy(
                        clips = track.clips.map { clip ->
                            if (clip.id == clipId) update(clip) else clip
                        }
                    )
                } else {
                    track
                }
            }
        )
        updateProject(updated)
    }

    private fun updateProject(project: VideoProject) {
        _uiState.update { it.copy(project = project) }
        viewModelScope.launch {
            repository.saveProject(project)
        }
        updatePreview()
    }

    private fun updatePreview() {
        val project = _uiState.value.project ?: return
        val position = _uiState.value.currentPosition

        viewModelScope.launch {
            val frame = renderingEngine.renderFrame(project, position)
            _uiState.update { it.copy(previewFrame = frame) }
        }
    }

    private fun startPlayback() {
        // TODO: Implement actual playback loop with frame rendering
    }

    private fun startAutoSave(project: VideoProject) {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(300000) // 5 minutes
                repository.autoSaveProject(project)
            }
        }
    }

    private fun loadAvailableResources() {
        _uiState.update {
            it.copy(
                availableTransitions = transitionEngine.getAvailableTransitions(),
                exportPresets = exportEngine.getAvailablePresets()
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        autoSaveJob?.cancel()
        renderingEngine.release()
    }
}
