package uc.ucworks.videosnap.domain.memory

import uc.ucworks.videosnap.util.Logger
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Pool for reusing byte arrays to reduce allocations for video frames
 */
class FramePool(
    private val frameSize: Int,
    private val maxPoolSize: Int = 20
) {
    companion object {
        private const val TAG = "FramePool"
    }

    private val pool = ConcurrentLinkedQueue<ByteArray>()
    private var allocatedCount = 0
    private var reusedCount = 0

    /**
     * Obtain a frame buffer from pool or allocate new one
     */
    fun obtain(): ByteArray {
        val buffer = pool.poll()
        return if (buffer != null) {
            reusedCount++
            Logger.d(TAG, "Reused frame buffer (reused: $reusedCount, pool: ${pool.size})")
            buffer
        } else {
            allocatedCount++
            Logger.d(TAG, "Allocated new frame buffer (total: $allocatedCount)")
            ByteArray(frameSize)
        }
    }

    /**
     * Return frame buffer to pool
     */
    fun recycle(buffer: ByteArray) {
        if (buffer.size != frameSize) {
            Logger.w(TAG, "Buffer size mismatch: ${buffer.size} vs $frameSize")
            return
        }

        if (pool.size < maxPoolSize) {
            pool.offer(buffer)
            Logger.d(TAG, "Recycled frame buffer (pool: ${pool.size})")
        } else {
            Logger.d(TAG, "Pool full, discarding buffer")
        }
    }

    /**
     * Clear the pool
     */
    fun clear() {
        pool.clear()
        Logger.d(TAG, "Pool cleared (allocated: $allocatedCount, reused: $reusedCount)")
        allocatedCount = 0
        reusedCount = 0
    }

    /**
     * Get pool statistics
     */
    fun getStats(): PoolStats {
        return PoolStats(
            poolSize = pool.size,
            allocatedCount = allocatedCount,
            reusedCount = reusedCount,
            reuseRate = if (allocatedCount > 0) {
                (reusedCount.toFloat() / (allocatedCount + reusedCount)) * 100
            } else 0f
        )
    }

    data class PoolStats(
        val poolSize: Int,
        val allocatedCount: Int,
        val reusedCount: Int,
        val reuseRate: Float
    )
}
