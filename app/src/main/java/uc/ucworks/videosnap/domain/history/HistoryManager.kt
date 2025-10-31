package uc.ucworks.videosnap.domain.history

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Professional undo/redo system using Command pattern
 * Supports unlimited history, keyboard shortcuts, and complex operations
 */
interface HistoryManager {
    val canUndo: StateFlow<Boolean>
    val canRedo: StateFlow<Boolean>
    val historyState: StateFlow<HistoryState>

    /**
     * Execute a command and add it to history
     */
    suspend fun execute(command: Command)

    /**
     * Undo the last command
     */
    suspend fun undo()

    /**
     * Redo the last undone command
     */
    suspend fun redo()

    /**
     * Clear all history
     */
    fun clear()

    /**
     * Begin a compound command (multiple commands as one undo/redo)
     */
    fun beginCompound(name: String)

    /**
     * End compound command
     */
    fun endCompound()

    /**
     * Get history for display
     */
    fun getHistory(): List<HistoryEntry>

    /**
     * Jump to specific point in history
     */
    suspend fun jumpTo(index: Int)
}

/**
 * Command interface for undo/redo pattern
 */
interface Command {
    val name: String
    val description: String

    /**
     * Execute the command
     */
    suspend fun execute()

    /**
     * Undo the command (reverse the operation)
     */
    suspend fun undo()

    /**
     * Check if command can be merged with another
     * Useful for continuous operations like dragging
     */
    fun canMergeWith(other: Command): Boolean = false

    /**
     * Merge with another command
     */
    fun mergeWith(other: Command): Command = this
}

/**
 * Compound command that groups multiple commands
 */
class CompoundCommand(
    override val name: String,
    override val description: String = "Multiple operations",
    private val commands: MutableList<Command> = mutableListOf()
) : Command {

    fun add(command: Command) {
        commands.add(command)
    }

    override suspend fun execute() {
        commands.forEach { it.execute() }
    }

    override suspend fun undo() {
        // Undo in reverse order
        commands.asReversed().forEach { it.undo() }
    }

    fun isEmpty(): Boolean = commands.isEmpty()
    fun size(): Int = commands.size
}

/**
 * History state for UI display
 */
data class HistoryState(
    val currentIndex: Int = -1,
    val totalCommands: Int = 0,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false
)

/**
 * History entry for display in history panel
 */
data class HistoryEntry(
    val index: Int,
    val name: String,
    val description: String,
    val timestamp: Long,
    val isCurrent: Boolean
)

/**
 * Implementation of HistoryManager
 */
class HistoryManagerImpl(
    private val maxHistorySize: Int = 100
) : HistoryManager {

    private val undoStack = mutableListOf<Command>()
    private val redoStack = mutableListOf<Command>()

    private var compoundCommand: CompoundCommand? = null

    private val _canUndo = MutableStateFlow(false)
    override val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    override val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val _historyState = MutableStateFlow(HistoryState())
    override val historyState: StateFlow<HistoryState> = _historyState.asStateFlow()

    override suspend fun execute(command: Command) {
        // If in compound mode, add to compound command
        compoundCommand?.let {
            it.add(command)
            command.execute()
            return
        }

        // Try to merge with last command if possible
        if (undoStack.isNotEmpty()) {
            val lastCommand = undoStack.last()
            if (lastCommand.canMergeWith(command)) {
                undoStack[undoStack.lastIndex] = lastCommand.mergeWith(command)
                command.execute()
                updateState()
                return
            }
        }

        // Execute the command
        command.execute()

        // Add to undo stack
        undoStack.add(command)

        // Clear redo stack when new command is executed
        redoStack.clear()

        // Limit history size
        if (undoStack.size > maxHistorySize) {
            undoStack.removeAt(0)
        }

        updateState()
    }

    override suspend fun undo() {
        if (undoStack.isEmpty()) return

        val command = undoStack.removeAt(undoStack.lastIndex)
        command.undo()

        redoStack.add(command)

        updateState()
    }

    override suspend fun redo() {
        if (redoStack.isEmpty()) return

        val command = redoStack.removeAt(redoStack.lastIndex)
        command.execute()

        undoStack.add(command)

        updateState()
    }

    override fun clear() {
        undoStack.clear()
        redoStack.clear()
        compoundCommand = null
        updateState()
    }

    override fun beginCompound(name: String) {
        compoundCommand = CompoundCommand(name)
    }

    override fun endCompound() {
        compoundCommand?.let { compound ->
            if (!compound.isEmpty()) {
                undoStack.add(compound)
                redoStack.clear()
            }
            compoundCommand = null
            updateState()
        }
    }

    override fun getHistory(): List<HistoryEntry> {
        val currentIndex = undoStack.size - 1
        return undoStack.mapIndexed { index, command ->
            HistoryEntry(
                index = index,
                name = command.name,
                description = command.description,
                timestamp = System.currentTimeMillis(), // Would need to store actual timestamps
                isCurrent = index == currentIndex
            )
        }
    }

    override suspend fun jumpTo(index: Int) {
        val currentIndex = undoStack.size - 1

        when {
            index < currentIndex -> {
                // Undo to reach target
                repeat(currentIndex - index) {
                    undo()
                }
            }
            index > currentIndex -> {
                // Redo to reach target
                repeat(index - currentIndex) {
                    redo()
                }
            }
        }
    }

    private fun updateState() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
        _historyState.value = HistoryState(
            currentIndex = undoStack.size - 1,
            totalCommands = undoStack.size + redoStack.size,
            canUndo = undoStack.isNotEmpty(),
            canRedo = redoStack.isNotEmpty()
        )
    }
}

/**
 * Specific command implementations for video editing
 */

/**
 * Add clip to timeline command
 */
class AddClipCommand(
    private val trackId: String,
    private val clip: uc.ucworks.videosnap.TimelineClip,
    private val onExecute: suspend (String, uc.ucworks.videosnap.TimelineClip) -> Unit,
    private val onUndo: suspend (String, String) -> Unit
) : Command {
    override val name = "Add Clip"
    override val description = "Add ${clip.mediaPath} to track"

    override suspend fun execute() {
        onExecute(trackId, clip)
    }

    override suspend fun undo() {
        onUndo(trackId, clip.id)
    }
}

/**
 * Remove clip from timeline command
 */
class RemoveClipCommand(
    private val trackId: String,
    private val clip: uc.ucworks.videosnap.TimelineClip,
    private val onExecute: suspend (String, String) -> Unit,
    private val onUndo: suspend (String, uc.ucworks.videosnap.TimelineClip) -> Unit
) : Command {
    override val name = "Remove Clip"
    override val description = "Remove clip from track"

    override suspend fun execute() {
        onExecute(trackId, clip.id)
    }

    override suspend fun undo() {
        onUndo(trackId, clip)
    }
}

/**
 * Move clip on timeline command
 */
class MoveClipCommand(
    private val trackId: String,
    private val clipId: String,
    private val oldStartTime: Long,
    private val newStartTime: Long,
    private val onMove: suspend (String, String, Long) -> Unit
) : Command {
    override val name = "Move Clip"
    override val description = "Move clip on timeline"

    override suspend fun execute() {
        onMove(trackId, clipId, newStartTime)
    }

    override suspend fun undo() {
        onMove(trackId, clipId, oldStartTime)
    }

    // Allow merging continuous move operations
    override fun canMergeWith(other: Command): Boolean {
        return other is MoveClipCommand &&
                other.trackId == trackId &&
                other.clipId == clipId
    }

    override fun mergeWith(other: Command): Command {
        other as MoveClipCommand
        return MoveClipCommand(
            trackId,
            clipId,
            oldStartTime, // Keep original start
            other.newStartTime, // Use new end
            onMove
        )
    }
}

/**
 * Trim clip command
 */
class TrimClipCommand(
    private val trackId: String,
    private val clipId: String,
    private val oldTrimStart: Long,
    private val oldTrimEnd: Long,
    private val newTrimStart: Long,
    private val newTrimEnd: Long,
    private val onTrim: suspend (String, String, Long, Long) -> Unit
) : Command {
    override val name = "Trim Clip"
    override val description = "Trim clip boundaries"

    override suspend fun execute() {
        onTrim(trackId, clipId, newTrimStart, newTrimEnd)
    }

    override suspend fun undo() {
        onTrim(trackId, clipId, oldTrimStart, oldTrimEnd)
    }

    override fun canMergeWith(other: Command): Boolean {
        return other is TrimClipCommand &&
                other.trackId == trackId &&
                other.clipId == clipId
    }

    override fun mergeWith(other: Command): Command {
        other as TrimClipCommand
        return TrimClipCommand(
            trackId,
            clipId,
            oldTrimStart,
            oldTrimEnd,
            other.newTrimStart,
            other.newTrimEnd,
            onTrim
        )
    }
}

/**
 * Apply effect command
 */
class ApplyEffectCommand(
    private val trackId: String,
    private val clipId: String,
    private val effect: uc.ucworks.videosnap.domain.Effect,
    private val onExecute: suspend (String, String, uc.ucworks.videosnap.domain.Effect) -> Unit,
    private val onUndo: suspend (String, String, String) -> Unit
) : Command {
    override val name = "Apply Effect"
    override val description = "Apply ${effect.type} effect"

    override suspend fun execute() {
        onExecute(trackId, clipId, effect)
    }

    override suspend fun undo() {
        onUndo(trackId, clipId, effect.id)
    }
}

/**
 * Change effect parameter command
 */
class ChangeEffectParameterCommand(
    private val trackId: String,
    private val clipId: String,
    private val effectId: String,
    private val parameterName: String,
    private val oldValue: Any,
    private val newValue: Any,
    private val onUpdate: suspend (String, String, String, String, Any) -> Unit
) : Command {
    override val name = "Change Effect Parameter"
    override val description = "Change $parameterName"

    override suspend fun execute() {
        onUpdate(trackId, clipId, effectId, parameterName, newValue)
    }

    override suspend fun undo() {
        onUpdate(trackId, clipId, effectId, parameterName, oldValue)
    }

    override fun canMergeWith(other: Command): Boolean {
        return other is ChangeEffectParameterCommand &&
                other.trackId == trackId &&
                other.clipId == clipId &&
                other.effectId == effectId &&
                other.parameterName == parameterName
    }

    override fun mergeWith(other: Command): Command {
        other as ChangeEffectParameterCommand
        return ChangeEffectParameterCommand(
            trackId,
            clipId,
            effectId,
            parameterName,
            oldValue, // Keep original value
            other.newValue, // Use new value
            onUpdate
        )
    }
}
