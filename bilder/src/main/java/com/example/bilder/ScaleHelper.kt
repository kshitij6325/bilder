package com.example.bilder

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.annotation.DrawableRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun getDownScaledBitmap(bitmap: Bitmap, width: Int?, height: Int?): Bitmap =
    withContext(Dispatchers.Default) {
        val (bmHeight: Float, bmWidth: Float) = bitmap.height.toFloat() to bitmap.width.toFloat()
        return@withContext if (height == null || width == null) bitmap
        else if (height > 0 && width > 0 && (height < bmHeight || width < bmWidth)) {
            if (width > height && bmWidth > width) {
                Bitmap.createScaledBitmap(
                    bitmap,
                    width,
                    ((bmHeight / bmWidth) * width).toInt(),
                    false
                )
            } else
                Bitmap.createScaledBitmap(
                    bitmap,
                    ((bmHeight / bmWidth) * height).toInt(),
                    height,
                    false
                )
        } else bitmap
    }

suspend fun getDownScaledBitmap(bmArray: ByteArray, width: Int?, height: Int?): Bitmap =
    withContext(Dispatchers.Default) {
        if (width == null || height == null) return@withContext BitmapFactory.decodeByteArray(
            bmArray,
            0,
            bmArray.size
        ) else
            return@withContext BitmapFactory.Options().run {
                inJustDecodeBounds = true
                BitmapFactory.decodeByteArray(bmArray, 0, bmArray.size)

                inSampleSize = calculateInSampleSize(this, width ?: 0, height ?: 0)

                inJustDecodeBounds = false

                BitmapFactory.decodeByteArray(bmArray, 0, bmArray.size)
            }
    }

suspend fun getDownScaledBitmap(
    res: Resources,
    @DrawableRes resId: Int,
    width: Int?,
    height: Int?
): Bitmap = withContext(Dispatchers.Default) {
    return@withContext if (width == null || height == null) BitmapFactory.decodeResource(
        res,
        resId
    ) else
        BitmapFactory.Options().run {
            inJustDecodeBounds = true
            BitmapFactory.decodeResource(res, resId)

            inSampleSize = calculateInSampleSize(this, width ?: 0, height ?: 0)

            inJustDecodeBounds = false

            BitmapFactory.decodeResource(res, resId)
        }
}

private fun calculateInSampleSize(
    options: BitmapFactory.Options,
    reqWidth: Int,
    reqHeight: Int
): Int {
    val (height: Int, width: Int) = options.run { outHeight to outWidth }
    var inSampleSize = 1

    if (height > reqHeight || width > reqWidth) {

        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2

        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }

    return inSampleSize
}
