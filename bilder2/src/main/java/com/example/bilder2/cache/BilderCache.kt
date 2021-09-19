package com.example.bilder2.cache

import android.content.Context
import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Singleton that handles and maintains cache of bitmaps using [InMemoryCache] for memory caching and
 * [DiskCache] for caching in disk.
 * */
internal object BilderCache : Cache<ByteArray, Bitmap?> {

    private val imMemoryCache = InMemoryCache
    private val diskCache = DiskCache

    private fun onImMemoryCacheEvict(key: String, bm: Bitmap?) {
        GlobalScope.launch(Dispatchers.Default) { diskCache.addAndGet(key, bm) }
    }

    override fun init(context: Context) = apply {
        diskCache.init(context)
        imMemoryCache.onEvict = this::onImMemoryCacheEvict
    }

    override suspend fun get(key: String): Bitmap? {
        return imMemoryCache.get(key) ?: diskCache.get(key)
    }

    override suspend fun addAndGet(key: String, bmData: ByteArray): Bitmap? {
        return imMemoryCache.addAndGet(key, bmData)
    }

    fun clearDiskCache() {
        GlobalScope.launch(Dispatchers.Default) { diskCache.clear() }
    }

    fun clearMemoryCache() {
        GlobalScope.launch(Dispatchers.Default) { imMemoryCache.clear() }
    }

    override val maxSize: Int
        get() = 0

    override fun getSize() = imMemoryCache.getSize() + diskCache.getSize()

    override suspend fun clear() {
        clearDiskCache()
        clearMemoryCache()
    }
}

const val KB = 1024
const val MB = 1024 * KB
