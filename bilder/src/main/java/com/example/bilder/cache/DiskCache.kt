package com.example.bilder.cache

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.example.bilder.log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

private val ROOT_DIR = "bilder"

/**
 * Implementation of [Cache] that caches bitmaps to disk.
 * */
internal class DiskCache(context: Context) : Cache<Bitmap?, Bitmap?> {

    private var rootDir = File("${context.cacheDir}/$ROOT_DIR").also {
        if (!it.exists()) {
            it.mkdir()
        }
        log("disk cache path is ${it.absolutePath}")
    }

    override val maxSize = ((Runtime.getRuntime().maxMemory() / MB) / 4).toInt()

    override fun getSize() =
        rootDir.listFiles()?.fold(0F, { acc, file -> acc + (file.length() / MB.toFloat()) })
            ?.toInt()
            ?: 0

    override suspend fun addAndGet(key: String, bmData: Bitmap?) =
        withContext(Dispatchers.Default) {
            if (isActive) {

                log("max size is ${maxSize}MB and current size is ${getSize()}MB")

                val bitmapOutputStream = ByteArrayOutputStream()
                bmData?.compress(Bitmap.CompressFormat.PNG, 0, bitmapOutputStream)
                val byteData = bitmapOutputStream.toByteArray()

                if (getSize() + (byteData.size / 100000F) > maxSize) {
                    cleanMemory(byteData.size)
                }

                val file = File("${rootDir.absolutePath}/$key")
                if (file.exists()) {
                    file.delete()
                }
                file.createNewFile()

                val fileOutputStream = FileOutputStream(file)
                fileOutputStream.write(byteData)
                fileOutputStream.flush()
                fileOutputStream.close()
                return@withContext bmData
            } else throw CancellationException()
        }

    override suspend fun get(key: String): Bitmap? = withContext(Dispatchers.Default) {
        return@withContext if (File("${rootDir.absolutePath}/$key").exists())
            BitmapFactory.decodeFile("${rootDir.absolutePath}/$key") else null
    }

    private fun cleanMemory(size: Int) {
        var memoryReleased = 0L
        rootDir.listFiles()?.forEach {
            if (memoryReleased < size) {
                memoryReleased += it.length()
                it.delete()
            } else {
                return@forEach
            }
            log("freed ${memoryReleased / KB}KB from disk")
        }
    }

    override suspend fun clear() {
        withContext(Dispatchers.Default) {
            var memoryReleased = 0L
            rootDir.listFiles()?.forEach {
                memoryReleased += it.length()
                it.delete()
            }
            log("freed ${memoryReleased / KB} KB from disk")
        }
    }

    override var onEvict: ((key: String, data: Bitmap?) -> Unit)? = null
}
