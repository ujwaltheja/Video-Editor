package uc.ucworks.videosnap.domain

import java.util.UUID

data class Effect(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val parameters: Map<String, Any> = emptyMap()
)

data class TransitionEffect(
    val id: String = UUID.randomUUID().toString(),
    val type: TransitionType,
    val duration: Long, // in milliseconds
    val position: TransitionPosition
)

enum class TransitionType {
    FADE,
    DISSOLVE,
    WIPE
}

enum class TransitionPosition {
    START,
    END
}
