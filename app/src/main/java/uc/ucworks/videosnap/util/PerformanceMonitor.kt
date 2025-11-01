package uc.ucworks.videosnap.util

import android.app.ActivityManager
import android.content.Context
import android.os.Debug
import android.os.Handler
import android.os.Looper
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

/**
 * Performance monitoring utility for FPS tracking and memory profiling
 */
class PerformanceMonitor(private val context: Context) {

    private val frameCount = AtomicInteger(0)
    private val lastFpsUpdate = AtomicLong(System.currentTimeMillis())
    private var currentFps: Float = 0f

    private val handler = Handler(Looper.getMainLooper())
    private var isMonitoring = false

    /**
     * Record a frame for FPS calculation
     */
    fun recordFrame() {
        frameCount.incrementAndGet()
        val now = System.currentTimeMillis()
        val elapsed = now - lastFpsUpdate.get()

        if (elapsed >= 1000) { // Update FPS every second
            currentFps = (frameCount.get() * 1000f) / elapsed
            frameCount.set(0)
            lastFpsUpdate.set(now)

            Logger.d("Performance", "FPS: ${"%.1f".format(currentFps)}")
        }
    }

    /**
     * Get current FPS
     */
    fun getCurrentFps(): Float = currentFps

    /**
     * Get memory info
     */
    fun getMemoryInfo(): MemoryInfo {
        val runtime = Runtime.getRuntime()
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        return MemoryInfo(
            usedMemoryMB = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024),
            totalMemoryMB = runtime.totalMemory() / (1024 * 1024),
            maxMemoryMB = runtime.maxMemory() / (1024 * 1024),
            availableMemoryMB = memInfo.availMem / (1024 * 1024),
            totalDeviceMemoryMB = memInfo.totalMem / (1024 * 1024),
            isLowMemory = memInfo.lowMemory,
            nativeHeapSizeMB = Debug.getNativeHeapSize() / (1024 * 1024),
            nativeHeapAllocatedMB = Debug.getNativeHeapAllocatedSize() / (1024 * 1024)
        )
    }

    /**
     * Log current memory status
     */
    fun logMemoryStatus() {
        val info = getMemoryInfo()
        Logger.memory("Performance", "App Memory", info.usedMemoryMB * 1024 * 1024)
        Logger.d("Performance",
            "Memory: ${info.usedMemoryMB}/${info.maxMemoryMB}MB | " +
            "Native: ${info.nativeHeapAllocatedMB}/${info.nativeHeapSizeMB}MB | " +
            "Low: ${info.isLowMemory}"
        )
    }

    /**
     * Start periodic memory monitoring
     */
    fun startMonitoring(intervalMs: Long = 5000) {
        if (isMonitoring) return
        isMonitoring = true

        handler.post(object : Runnable {
            override fun run() {
                if (!isMonitoring) return
                logMemoryStatus()
                handler.postDelayed(this, intervalMs)
            }
        })
    }

    /**
     * Stop monitoring
     */
    fun stopMonitoring() {
        isMonitoring = false
        handler.removeCallbacksAndMessages(null)
    }

    /**
     * Check if we're in low memory situation
     */
    fun isLowMemory(): Boolean {
        val info = getMemoryInfo()
        val usagePercent = (info.usedMemoryMB.toFloat() / info.maxMemoryMB) * 100
        return info.isLowMemory || usagePercent > 85
    }

    /**
     * Get memory usage percentage
     */
    fun getMemoryUsagePercent(): Float {
        val info = getMemoryInfo()
        return (info.usedMemoryMB.toFloat() / info.maxMemoryMB) * 100
    }

    data class MemoryInfo(
        val usedMemoryMB: Long,
        val totalMemoryMB: Long,
        val maxMemoryMB: Long,
        val availableMemoryMB: Long,
        val totalDeviceMemoryMB: Long,
        val isLowMemory: Boolean,
        val nativeHeapSizeMB: Long,
        val nativeHeapAllocatedMB: Long
    )

    companion object {
        private var instance: PerformanceMonitor? = null

        fun getInstance(context: Context): PerformanceMonitor {
            return instance ?: synchronized(this) {
                instance ?: PerformanceMonitor(context.applicationContext).also { instance = it }
            }
        }
    }
}
