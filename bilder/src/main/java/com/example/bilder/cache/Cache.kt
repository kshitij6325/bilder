package com.example.bilder.cache

/**
 * Base interface class for cache.
 * [Input] type of input that the method [addAndGet] will process.
 * [Output] type of output that the method [get] will return.
 * */
internal interface Cache<Input, Output> {

    /**
     * Max size of cache.
     * */
    val maxSize: Int

    /**
     * Current size of cache.
     * */
    fun getSize(): Int

    /**
     * Add data to cache.
     * */
    suspend fun addAndGet(key: String, bmData: Input): Output

    /**
     * Get data from cache.
     * */
    suspend fun get(key: String): Output

    /**
     * Clear the cache.
     * */
    suspend fun clear()

    var onEvict: ((key: String, data: Output) -> Unit)?
}
