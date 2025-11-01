package uc.ucworks.videosnap.domain.memory

import android.graphics.Bitmap
import uc.ucworks.videosnap.util.Logger
import java.util.concurrent.ConcurrentHashMap

/**
 * Pool for reusing Bitmap objects to reduce memory allocations
 */
class BitmapPool(private val maxPoolSizeMB: Int = 50) {

    companion object {
        private const val TAG = "BitmapPool"
    }

    private val pool = ConcurrentHashMap<String, MutableList<Bitmap>>()
    private var currentSizeMB = 0

    /**
     * Get bitmap from pool or create new one
     */
    fun obtain(width: Int, height: Int, config: Bitmap.Config = Bitmap.Config.ARGB_8888): Bitmap {
        val key = getKey(width, height, config)
        val bitmaps = pool[key]

        val bitmap = bitmaps?.removeFirstOrNull()
        return if (bitmap != null && !bitmap.isRecycled) {
            updateSize(-getBitmapSizeMB(bitmap))
            Logger.d(TAG, "Reused bitmap ${width}x${height} (pool: ${currentSizeMB}MB)")
            bitmap
        } else {
            val newBitmap = Bitmap.createBitmap(width, height, config)
            Logger.d(TAG, "Created new bitmap ${width}x${height}")
            newBitmap
        }
    }

    /**
     * Return bitmap to pool
     */
    fun recycle(bitmap: Bitmap) {
        if (bitmap.isRecycled) {
            return
        }

        val sizeMB = getBitmapSizeMB(bitmap)
        if (currentSizeMB + sizeMB > maxPoolSizeMB) {
            bitmap.recycle()
            Logger.d(TAG, "Pool full, recycling bitmap")
            return
        }

        val key = getKey(bitmap.width, bitmap.height, bitmap.config)
        val bitmaps = pool.getOrPut(key) { mutableListOf() }

        bitmaps.add(bitmap)
        updateSize(sizeMB)
        Logger.d(TAG, "Recycled bitmap to pool (pool: ${currentSizeMB}MB)")
    }

    /**
     * Clear the pool
     */
    fun clear() {
        pool.values.forEach { bitmaps ->
            bitmaps.forEach { it.recycle() }
        }
        pool.clear()
        currentSizeMB = 0
        Logger.d(TAG, "Pool cleared")
    }

    /**
     * Trim pool to target size
     */
    fun trim(targetSizeMB: Int) {
        while (currentSizeMB > targetSizeMB && pool.isNotEmpty()) {
            val firstKey = pool.keys.firstOrNull() ?: break
            val bitmaps = pool[firstKey] ?: continue

            if (bitmaps.isNotEmpty()) {
                val bitmap = bitmaps.removeFirst()
                updateSize(-getBitmapSizeMB(bitmap))
                bitmap.recycle()
            }

            if (bitmaps.isEmpty()) {
                pool.remove(firstKey)
            }
        }
        Logger.d(TAG, "Trimmed pool to ${currentSizeMB}MB")
    }

    private fun getKey(width: Int, height: Int, config: Bitmap.Config): String {
        return "${width}x${height}_${config.name}"
    }

    private fun getBitmapSizeMB(bitmap: Bitmap): Int {
        return bitmap.byteCount / (1024 * 1024)
    }

    private fun updateSize(deltaMB: Int) {
        currentSizeMB += deltaMB
    }

    fun getCurrentSizeMB(): Int = currentSizeMB
}
