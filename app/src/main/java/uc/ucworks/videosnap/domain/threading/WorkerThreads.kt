package uc.ucworks.videosnap.domain.threading

import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import uc.ucworks.videosnap.util.Logger
import java.util.concurrent.*
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.asCoroutineDispatcher

/**
 * Dedicated worker threads for video editor operations
 */
object WorkerThreads {

    private const val TAG = "WorkerThreads"

    // Rendering thread - for OpenGL operations
    private val renderingThread = HandlerThread("RenderingThread").apply {
        start()
    }
    val renderingHandler = Handler(renderingThread.looper)
    val renderingLooper: Looper = renderingThread.looper

    // Decoding thread - for video frame extraction
    private val decodingExecutor = Executors.newFixedThreadPool(
        2,
        ThreadFactory { r -> Thread(r, "DecodingThread") }
    )
    val decodingDispatcher: CoroutineContext = decodingExecutor.asCoroutineDispatcher()

    // Encoding thread - for export processing
    private val encodingExecutor = Executors.newSingleThreadExecutor(
        ThreadFactory { r -> Thread(r, "EncodingThread") }
    )
    val encodingDispatcher: CoroutineContext = encodingExecutor.asCoroutineDispatcher()

    // Audio mixing thread
    private val audioExecutor = Executors.newSingleThreadExecutor(
        ThreadFactory { r -> Thread(r, "AudioThread") }
    )
    val audioDispatcher: CoroutineContext = audioExecutor.asCoroutineDispatcher()

    // Background processing thread pool
    private val backgroundExecutor = Executors.newFixedThreadPool(
        4,
        ThreadFactory { r -> Thread(r, "BackgroundThread") }
    )
    val backgroundDispatcher: CoroutineContext = backgroundExecutor.asCoroutineDispatcher()

    /**
     * Execute on rendering thread
     */
    fun runOnRenderingThread(block: () -> Unit) {
        renderingHandler.post(block)
    }

    /**
     * Execute on rendering thread and wait for completion
     */
    fun runOnRenderingThreadBlocking(block: () -> Unit) {
        val latch = CountDownLatch(1)
        var exception: Throwable? = null

        renderingHandler.post {
            try {
                block()
            } catch (e: Throwable) {
                exception = e
            } finally {
                latch.countDown()
            }
        }

        latch.await()
        exception?.let { throw it }
    }

    /**
     * Shutdown all worker threads
     */
    fun shutdown() {
        Logger.d(TAG, "Shutting down worker threads")

        renderingThread.quitSafely()
        decodingExecutor.shutdown()
        encodingExecutor.shutdown()
        audioExecutor.shutdown()
        backgroundExecutor.shutdown()

        try {
            decodingExecutor.awaitTermination(5, TimeUnit.SECONDS)
            encodingExecutor.awaitTermination(5, TimeUnit.SECONDS)
            audioExecutor.awaitTermination(5, TimeUnit.SECONDS)
            backgroundExecutor.awaitTermination(5, TimeUnit.SECONDS)
        } catch (e: InterruptedException) {
            Logger.e(TAG, "Interrupted while waiting for thread shutdown", e)
        }

        Logger.d(TAG, "All worker threads shut down")
    }

    /**
     * Get thread pool info for debugging
     */
    fun getThreadPoolInfo(): ThreadPoolInfo {
        return ThreadPoolInfo(
            renderingThreadAlive = renderingThread.isAlive,
            decodingPoolActive = !decodingExecutor.isShutdown,
            encodingPoolActive = !encodingExecutor.isShutdown,
            audioPoolActive = !audioExecutor.isShutdown,
            backgroundPoolActive = !backgroundExecutor.isShutdown
        )
    }

    data class ThreadPoolInfo(
        val renderingThreadAlive: Boolean,
        val decodingPoolActive: Boolean,
        val encodingPoolActive: Boolean,
        val audioPoolActive: Boolean,
        val backgroundPoolActive: Boolean
    )
}
