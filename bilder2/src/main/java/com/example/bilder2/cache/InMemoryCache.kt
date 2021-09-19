package com.example.bilder2.cache

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.lang.ref.SoftReference
import java.util.*
import kotlin.collections.HashSet

/**
 * Implementation of [Cache] that caches bitmaps in memory using [LruCache].
 * */
internal object InMemoryCache : Cache<ByteArray, Bitmap?> {

    /**
     * [LruCache] for caching bitmaps.
     * */
    private val cache by lazy {
        initializeCache(maxSize)
    }

    var onEvict: ((String, Bitmap?) -> Unit)? = null

    /**
     * When evicting any bitmap from cache, they are stored here to check if they can be reused using
     * [BitmapFactory.Options.inBitmap]
     * */
    private val reusableBitmaps: MutableSet<SoftReference<Bitmap>> by lazy {
        Collections.synchronizedSet(HashSet<SoftReference<Bitmap>>())
    }

    override val maxSize = ((Runtime.getRuntime().maxMemory() / KB) / 8).toInt()

    override fun getSize() = cache.size()

    /**
     * Returns bitmap from a given key from [cache]
     * */
    override suspend fun get(key: String): Bitmap? = withContext(Dispatchers.Default) {
        return@withContext cache.get(key)
    }

    override suspend fun clear() {
        withContext(Dispatchers.Default) {
            cache.evictAll()
        }
    }

    private fun initializeCache(cacheSize: Int) = object : LruCache<String, Bitmap>(cacheSize) {

        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024
        }

        override fun entryRemoved(
            evicted: Boolean,
            key: String?,
            oldValue: Bitmap?,
            newValue: Bitmap?
        ) {
            key?.run { onEvict?.invoke(key, oldValue) }
            reusableBitmaps.add(SoftReference(oldValue))
        }
    }

    /**
     * Reuse bitmap if possible from [reusableBitmaps] instead of creating a new Bitmap object everytime.
     * */
    private suspend fun getReusedBitmapIfPossible(key: String, byteArray: ByteArray, len: Int) =
        withContext(Dispatchers.Default) {
            return@withContext if (isActive) BitmapFactory.Options().run {
                inMutable = true
                inSampleSize = 1
                inBitmap = getBitmapFromReusableSet(this)
                BitmapFactory.decodeByteArray(byteArray, 0, len, this)?.also {
                    cache.put(key, it)
                }
            } else throw CancellationException()
        }

    /**
     * Used in [getReusedBitmapIfPossible] to iterate [reusableBitmaps] and check if it has any
     * bitmap with appropriate size that can be reused
     * */
    private fun getBitmapFromReusableSet(options: BitmapFactory.Options): Bitmap? {
        reusableBitmaps.takeIf { it.isNotEmpty() }?.let { reusableBitmaps ->
            synchronized(reusableBitmaps) {
                val iterator: MutableIterator<SoftReference<Bitmap>> = reusableBitmaps.iterator()
                while (iterator.hasNext()) {
                    iterator.next().get()?.let { item ->
                        if (item.isMutable) {
                            if (canUseForInBitmap(item, options)) {
                                iterator.remove()
                                return item
                            }
                        } else {
                            iterator.remove()
                        }
                    }
                }
            }
        }
        return null
    }

    /**
     * Check if bitmap size required is <= bitmap size available. If yes, this bitmap can be reused.
     * */
    private fun canUseForInBitmap(
        candidate: Bitmap,
        targetOptions: BitmapFactory.Options
    ): Boolean {
        val width: Int = targetOptions.outWidth / targetOptions.inSampleSize
        val height: Int = targetOptions.outHeight / targetOptions.inSampleSize
        val byteCount: Int = width * height * getBytesPerPixel(candidate.config)
        return byteCount <= candidate.allocationByteCount
    }

    private fun getBytesPerPixel(config: Bitmap.Config): Int {
        return when (config) {
            Bitmap.Config.ARGB_8888 -> 4
            Bitmap.Config.RGB_565 -> 2
            Bitmap.Config.ALPHA_8 -> 1
            else -> 1
        }
    }

    override suspend fun addAndGet(key: String, bmData: ByteArray) =
        getReusedBitmapIfPossible(key, bmData, bmData.size)

    override fun init(context: Context) = this
}
