package com.example.bilder.cache

import android.graphics.Bitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * Singleton that handles and maintains cache of bitmaps using [InMemoryCache] for memory caching and
 * [DiskCache] for caching on disk.
 * */
internal class BilderCache(
    private val inMemoryCache: Cache<Bitmap?, Bitmap?>,
    private val diskCache: Cache<Bitmap?, Bitmap?>
) : Cache<Bitmap?, Bitmap?> {

    init {
        inMemoryCache.onEvict = { key: String, bm: Bitmap? ->
            GlobalScope.launch(Dispatchers.Default) { diskCache.addAndGet(key, bm) }
        }
    }

    override suspend fun get(key: String): Bitmap? {
        return inMemoryCache.get(key) ?: diskCache.get(key)
    }

    override suspend fun addAndGet(key: String, bmData: Bitmap?): Bitmap? {
        return inMemoryCache.addAndGet(key, bmData)
    }

    fun clearDiskCache() {
        GlobalScope.launch(Dispatchers.Default) { diskCache.clear() }
    }

    fun clearMemoryCache() {
        GlobalScope.launch(Dispatchers.Default) { inMemoryCache.clear() }
    }

    override fun getSize() = inMemoryCache.getSize() + diskCache.getSize()

    override suspend fun clear() {
        clearDiskCache()
        clearMemoryCache()
    }

    override val maxSize = 0
    override var onEvict: ((String, Bitmap?) -> Unit)? = null
}

const val KB = 1024
const val MB = 1024 * KB
