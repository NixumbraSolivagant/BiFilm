package com.bifilm.app.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

class ImageStore(private val context: Context) {

    private val baseDir: File =
        File(context.filesDir, "bifilm").apply { if (!exists()) mkdirs() }

    private val projectDir: File =
        File(baseDir, "projects").apply { if (!exists()) mkdirs() }

    private val layerDir: File =
        File(baseDir, "layers").apply { if (!exists()) mkdirs() }

    fun newProjectDir(projectId: String): File {
        val dir = File(projectDir, projectId)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun layerFile(projectId: String, layerId: String = UUID.randomUUID().toString()): File {
        val dir = newProjectDir(projectId)
        return File(dir, "layer-$layerId.jpg")
    }

    fun maskFile(projectId: String, layerId: String): File {
        val dir = newProjectDir(projectId)
        return File(dir, "mask-$layerId.png")
    }

    fun thumbnailFile(projectId: String): File {
        return File(newProjectDir(projectId), "thumb.jpg")
    }

    fun copyUriToLayerFile(sourceUri: Uri, projectId: String): File {
        val target = layerFile(projectId)
        context.contentResolver.openInputStream(sourceUri).use { input ->
            requireNotNull(input) { "Cannot open input stream for $sourceUri" }
            FileOutputStream(target).use { output ->
                input.copyTo(output)
            }
        }
        return target
    }

    fun writeBitmapToLayerFile(bitmap: Bitmap, projectId: String, quality: Int = 92): File {
        val target = layerFile(projectId)
        FileOutputStream(target).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
        }
        return target
    }

    fun writeThumbnail(bitmap: Bitmap, projectId: String): File {
        val target = thumbnailFile(projectId)
        FileOutputStream(target).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        return target
    }

    companion object {
        fun decodeRespectingOrientation(file: File, maxLongEdge: Int = 1080): Bitmap? {
            val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, opts)
            val longEdge = maxOf(opts.outWidth, opts.outHeight)
            var inSample = 1
            while (longEdge / inSample > maxLongEdge) inSample *= 2
            val decode = BitmapFactory.Options().apply { inSampleSize = inSample }
            val raw = BitmapFactory.decodeFile(file.absolutePath, decode) ?: return null
            val orientation = runCatching {
                ExifInterface(file.absolutePath)
                    .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> return raw
            }
            val rotated = Bitmap.createBitmap(raw, 0, 0, raw.width, raw.height, matrix, true)
            if (rotated !== raw) raw.recycle()
            return rotated
        }
    }
}
