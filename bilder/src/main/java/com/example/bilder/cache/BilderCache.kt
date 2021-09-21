package com.example.bilder.cache

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Singleton that handles and maintains cache of bitmaps using [InMemoryCache] for memory caching and
 * [DiskCache] for caching in disk.
 * */
internal class BilderCache(context: Context) : Cache<Bitmap, Bitmap?> {

    private val imMemoryCache = InMemoryCache()
    private val diskCache = DiskCache(context)

    init {
        imMemoryCache.onEvict = { key: String, bm: Bitmap? ->
            GlobalScope.launch(Dispatchers.Default) { diskCache.addAndGet(key, bm) }
        }
    }

    override suspend fun get(key: String): Bitmap? {
        return imMemoryCache.get(key) ?: diskCache.get(key)
    }

    override suspend fun addAndGet(key: String, bmData: Bitmap): Bitmap? {
        return imMemoryCache.addAndGet(key, bmData)
    }

    fun clearDiskCache() {
        GlobalScope.launch(Dispatchers.Default) { diskCache.clear() }
    }

    fun clearMemoryCache() {
        GlobalScope.launch(Dispatchers.Default) { imMemoryCache.clear() }
    }

    override fun getSize() = imMemoryCache.getSize() + diskCache.getSize()

    override suspend fun clear() {
        clearDiskCache()
        clearMemoryCache()
    }

    override val maxSize = 0
    override var onEvict: ((String, Bitmap?) -> Unit)? = null
}

const val KB = 1024
const val MB = 1024 * KB
