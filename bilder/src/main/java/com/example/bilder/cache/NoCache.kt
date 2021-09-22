package com.example.bilder.cache

import android.graphics.Bitmap

internal class NoCache : Cache<Bitmap?, Bitmap?> {

    override val maxSize = 0

    override fun getSize() = 0

    override suspend fun addAndGet(key: String, bmData: Bitmap?) = bmData

    override suspend fun get(key: String): Bitmap? = null

    override suspend fun clear() {
    }

    override var onEvict: ((key: String, data: Bitmap?) -> Unit)? = null
}
