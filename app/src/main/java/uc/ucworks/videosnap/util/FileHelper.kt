package uc.ucworks.videosnap.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * File I/O operations wrapper with error handling
 */
object FileHelper {

    /**
     * Get file name from URI
     */
    fun getFileName(context: Context, uri: Uri): String? {
        var fileName: String? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex)
                }
            }
        }
        return fileName ?: uri.lastPathSegment
    }

    /**
     * Get file size from URI in bytes
     */
    fun getFileSize(context: Context, uri: Uri): Long {
        var size: Long = 0
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1) {
                    size = cursor.getLong(sizeIndex)
                }
            }
        }
        return size
    }

    /**
     * Copy file from URI to internal cache
     */
    fun copyToCache(context: Context, uri: Uri, fileName: String): Result<File> {
        return try {
            val cacheFile = File(context.cacheDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(cacheFile).use { output ->
                    input.copyTo(output)
                }
            } ?: return Result.failure(IOException("Cannot open input stream"))

            Result.success(cacheFile)
        } catch (e: Exception) {
            Logger.e("FileHelper", "Failed to copy file to cache", e)
            Result.failure(e)
        }
    }

    /**
     * Create temporary file in cache directory
     */
    fun createTempFile(context: Context, prefix: String, suffix: String): Result<File> {
        return try {
            val tempFile = File.createTempFile(prefix, suffix, context.cacheDir)
            Result.success(tempFile)
        } catch (e: Exception) {
            Logger.e("FileHelper", "Failed to create temp file", e)
            Result.failure(e)
        }
    }

    /**
     * Delete file safely
     */
    fun deleteFile(file: File): Boolean {
        return try {
            if (file.exists()) {
                file.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            Logger.e("FileHelper", "Failed to delete file: ${file.path}", e)
            false
        }
    }

    /**
     * Clean up temporary files older than specified age
     */
    fun cleanupTempFiles(context: Context, maxAgeMs: Long = 24 * 60 * 60 * 1000L): Int {
        var deletedCount = 0
        val now = System.currentTimeMillis()

        context.cacheDir.listFiles()?.forEach { file ->
            if (file.isFile && (now - file.lastModified()) > maxAgeMs) {
                if (deleteFile(file)) {
                    deletedCount++
                }
            }
        }

        Logger.d("FileHelper", "Cleaned up $deletedCount temp files")
        return deletedCount
    }

    /**
     * Get cache directory size in bytes
     */
    fun getCacheSize(context: Context): Long {
        return context.cacheDir.walkTopDown()
            .filter { it.isFile }
            .map { it.length() }
            .sum()
    }

    /**
     * Format bytes to human-readable size
     */
    fun formatFileSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0

        return when {
            gb >= 1 -> "%.2f GB".format(gb)
            mb >= 1 -> "%.2f MB".format(mb)
            kb >= 1 -> "%.2f KB".format(kb)
            else -> "$bytes B"
        }
    }

    /**
     * Copy input stream to file
     */
    fun copyStreamToFile(inputStream: InputStream, outputFile: File): Result<Unit> {
        return try {
            FileOutputStream(outputFile).use { output ->
                inputStream.copyTo(output)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Logger.e("FileHelper", "Failed to copy stream to file", e)
            Result.failure(e)
        }
    }

    /**
     * Ensure directory exists, create if not
     */
    fun ensureDirectoryExists(directory: File): Boolean {
        return if (!directory.exists()) {
            directory.mkdirs()
        } else {
            directory.isDirectory
        }
    }
}
