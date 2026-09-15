package com.benedy.deudas.ui.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * Stores client avatars under filesDir/client_photos/.
 * [ClientEntity.photoPath] holds a relative path like "client_photos/uuid.jpg".
 */
object ClientPhotoHelper {
    const val DIR_NAME = "client_photos"
    private const val AUTHORITY_SUFFIX = ".fileprovider"

    fun photosDir(context: Context): File {
        val dir = File(context.filesDir, DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun resolveFile(context: Context, photoPath: String?): File? {
        if (photoPath.isNullOrBlank()) return null
        val file = if (photoPath.startsWith("/")) {
            File(photoPath)
        } else {
            File(context.filesDir, photoPath)
        }
        return file.takeIf { it.exists() && it.isFile }
    }

    fun resolveUri(context: Context, photoPath: String?): Uri? {
        val file = resolveFile(context, photoPath) ?: return null
        return Uri.fromFile(file)
    }

    /** Temp file for camera capture (before copy into permanent storage). */
    fun createCameraTempFile(context: Context): File {
        val dir = File(context.cacheDir, "camera_capture").also { it.mkdirs() }
        return File(dir, "capture_${System.currentTimeMillis()}.jpg")
    }

    fun cameraTempUri(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}$AUTHORITY_SUFFIX",
            file
        )
    }

    /**
     * Copies [source] into filesDir/client_photos/ and returns relative path.
     * Deletes [previousRelativePath] if different.
     */
    fun persistPhoto(
        context: Context,
        source: Uri,
        previousRelativePath: String? = null
    ): String {
        val dest = File(photosDir(context), "${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(source)?.use { input ->
            FileOutputStream(dest).use { output -> input.copyTo(output) }
        } ?: error("No se pudo leer la foto")
        if (!previousRelativePath.isNullOrBlank() &&
            previousRelativePath != relativePath(dest)
        ) {
            deletePhoto(context, previousRelativePath)
        }
        return relativePath(dest)
    }

    fun persistFromFile(
        context: Context,
        sourceFile: File,
        previousRelativePath: String? = null
    ): String {
        val dest = File(photosDir(context), "${UUID.randomUUID()}.jpg")
        sourceFile.copyTo(dest, overwrite = true)
        if (sourceFile.exists() && sourceFile.absolutePath != dest.absolutePath) {
            sourceFile.delete()
        }
        if (!previousRelativePath.isNullOrBlank() &&
            previousRelativePath != relativePath(dest)
        ) {
            deletePhoto(context, previousRelativePath)
        }
        return relativePath(dest)
    }

    fun deletePhoto(context: Context, photoPath: String?) {
        resolveFile(context, photoPath)?.delete()
    }

    fun relativePath(file: File): String = "$DIR_NAME/${file.name}"
}
