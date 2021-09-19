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
internal object BilderCache : Cache<ByteArray, Bitmap?> {

    private val imMemoryCache = InMemoryCache
    private val diskCache = DiskCache

    private fun onImMemoryCacheEvict(key: String, bm: Bitmap?) {
        GlobalScope.launch(Dispatchers.Default) { DiskCache.addAndGet(key, bm) }
    }

    override fun init(context: Context) = apply {
        DiskCache.init(context)
        InMemoryCache.onEvict = this::onImMemoryCacheEvict
    }

    override suspend fun get(key: String): Bitmap? {
        return InMemoryCache.get(key) ?: DiskCache.get(key)
    }

    override suspend fun addAndGet(key: String, bmData: ByteArray): Bitmap? {
        return InMemoryCache.addAndGet(key, bmData)
    }

    fun clearDiskCache() {
        GlobalScope.launch(Dispatchers.Default) { DiskCache.clear() }
    }

    fun clearMemoryCache() {
        GlobalScope.launch(Dispatchers.Default) { InMemoryCache.clear() }
    }

    override val maxSize: Int
        get() = 0

    override fun getSize() = InMemoryCache.getSize() + DiskCache.getSize()

    override suspend fun clear() {
        clearDiskCache()
        clearMemoryCache()
    }
}

const val KB = 1024
const val MB = 1024 * KB
