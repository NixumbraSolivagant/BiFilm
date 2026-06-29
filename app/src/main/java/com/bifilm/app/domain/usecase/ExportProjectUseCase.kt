package com.bifilm.app.domain.usecase

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.bifilm.app.data.image.ImageStore
import com.bifilm.app.util.Logger

/**
 * 把合成出来的 Bitmap 导出为 JPEG 到相册 MediaStore.Photos.
 * 返回保存后的 Uri 或 null.
 */
class ExportProjectUseCase(
    private val context: Context,
    private val imageStore: ImageStore,
    private val maxLongEdgePx: Int = 1080
) {
    suspend fun exportToGallery(bitmap: Bitmap, title: String): Uri? {
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$title-${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/BiFilm")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(collection, values) ?: run {
            Logger.e(TAG, "MediaStore.insert returned null")
            return null
        }
        resolver.openOutputStream(uri)?.use { out ->
            val sized = if (maxOf(bitmap.width, bitmap.height) > maxLongEdgePx) {
                val scale = maxLongEdgePx.toFloat() / maxOf(bitmap.width, bitmap.height)
                Bitmap.createScaledBitmap(
                    bitmap,
                    (bitmap.width * scale).toInt(),
                    (bitmap.height * scale).toInt(),
                    true
                )
            } else bitmap
            sized.compress(Bitmap.CompressFormat.JPEG, 92, out)
            if (sized !== bitmap) sized.recycle()
        } ?: run {
            resolver.delete(uri, null, null)
            Logger.e(TAG, "openOutputStream failed")
            return null
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        Logger.d(TAG, "exported to $uri")
        return uri
    }

    companion object {
        private const val TAG = "ExportProjectUseCase"
    }
}