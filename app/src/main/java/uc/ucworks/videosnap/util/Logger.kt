package uc.ucworks.videosnap.util

import android.util.Log
import uc.ucworks.videosnap.BuildConfig

/**
 * Centralized logging utility with automatic debug/release level management.
 * Logs are only printed in debug builds.
 */
object Logger {
    private const val DEFAULT_TAG = "VideoEditor"

    var isDebugMode = BuildConfig.DEBUG

    fun d(tag: String = DEFAULT_TAG, message: String) {
        if (isDebugMode) {
            Log.d(tag, message)
        }
    }

    fun d(message: String) = d(DEFAULT_TAG, message)

    fun i(tag: String = DEFAULT_TAG, message: String) {
        if (isDebugMode) {
            Log.i(tag, message)
        }
    }

    fun i(message: String) = i(DEFAULT_TAG, message)

    fun w(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        if (isDebugMode) {
            if (throwable != null) {
                Log.w(tag, message, throwable)
            } else {
                Log.w(tag, message)
            }
        }
    }

    fun w(message: String, throwable: Throwable? = null) = w(DEFAULT_TAG, message, throwable)

    fun e(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null) {
        // Always log errors, even in release builds
        if (throwable != null) {
            Log.e(tag, message, throwable)
        } else {
            Log.e(tag, message)
        }
    }

    fun e(message: String, throwable: Throwable? = null) = e(DEFAULT_TAG, message, throwable)

    /**
     * Log performance metrics
     */
    fun perf(tag: String = DEFAULT_TAG, operation: String, durationMs: Long) {
        if (isDebugMode) {
            Log.i(tag, "⏱️ $operation took ${durationMs}ms")
        }
    }

    /**
     * Log memory usage
     */
    fun memory(tag: String = DEFAULT_TAG, message: String, bytes: Long) {
        if (isDebugMode) {
            val mb = bytes / (1024 * 1024)
            Log.d(tag, "💾 $message: ${mb}MB")
        }
    }

    /**
     * Measure and log execution time of a block
     */
    inline fun <T> measureTime(tag: String = DEFAULT_TAG, operation: String, block: () -> T): T {
        val start = System.currentTimeMillis()
        return try {
            block()
        } finally {
            val duration = System.currentTimeMillis() - start
            perf(tag, operation, duration)
        }
    }
}
