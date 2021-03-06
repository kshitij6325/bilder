package com.example.bilder

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.example.bilder.cache.*
import com.example.bilder.cache.Cache
import com.example.bilder.network.BilderDownloader
import com.example.bilder.network.DownloadRequest
import kotlinx.coroutines.*

private const val TAG_ERROR = "**Bilder Error**"
private const val TAG_DEBUG = "**Bilder Debug**"

object Bilder {

    /**
     * Parent scope for creating each load request coroutine.
     * */
    private var scope =
        CoroutineScope(Dispatchers.Main)

    /**
     * For Downloading images from server.
     * */
    private val imageDownloader = BilderDownloader()

    /**
     * For caching bitmaps.
     * */
    private var mixCache: Cache<Bitmap?, Bitmap?>? = null
    private var inMemoryCache: Cache<Bitmap?, Bitmap?>? = null
    private var diskCache: Cache<Bitmap?, Bitmap?>? = null

    private var areCacheInitialized = false

    class Config {

        var disableMemoryCache: Boolean = false
        var disableDiskCache: Boolean = false

        var onBitmapLoaded: ((Bitmap) -> Unit)? = null
        var onBitmapLoadFailure: ((java.lang.Exception) -> Unit)? = null

        fun configure(configBuilder: Config.() -> Unit) = apply(configBuilder)

        /**
         * Loads image from source.
         *
         * @param source [Source] of the image to be loaded.
         * @param placeHolder drawable id for placeholder image
         * @return [Task] for the current request
         * */
        fun load(
            source: Source,
            imageView: ImageView? = null,
            @DrawableRes
            placeHolder: Int? = null
        ) = Task(
            kotlin.run {
                val imageCache = when {
                    disableMemoryCache && !disableDiskCache -> diskCache
                    !disableDiskCache && disableMemoryCache -> inMemoryCache
                    !disableDiskCache && !disableMemoryCache -> mixCache
                    else -> null
                }

                imageView?.setImageResource(placeHolder ?: 0)
                scope.launch {
                    val key = geKey(source)
                    prepareImageView(imageView, this)
                    imageCache?.get(key)?.also {
                        imageView?.setImageBitmap(it)
                        onBitmapLoaded?.invoke(it)
                    } ?: run {
                        when (source) {
                            is Source.Url -> {
                                imageDownloader.download(source.src).run {
                                    when (this) {
                                        is DownloadRequest.Success -> {
                                            getDownScaledBitmap(
                                                data,
                                                imageView?.width,
                                                imageView?.height
                                            ).also { bm ->
                                                imageCache?.addAndGet(key, bm)
                                                imageView?.setImageBitmap(bm)
                                                onBitmapLoaded?.invoke(bm)
                                            }
                                        }
                                        is DownloadRequest.Error -> {
                                            onBitmapLoadFailure?.invoke(e)
                                        }
                                        is DownloadRequest.Canceled -> {
                                        }
                                    }
                                }
                            }
                            is Source.Bitmap -> {
                                getDownScaledBitmap(
                                    source.src,
                                    imageView?.width,
                                    imageView?.height
                                ).also { bm ->
                                    imageCache?.addAndGet(key, bm)
                                    imageView?.setImageBitmap(bm)
                                    onBitmapLoaded?.invoke(bm)
                                }
                            }
                            is Source.DrawableRes -> {
                                getDownScaledBitmap(
                                    imageView?.context?.resources,
                                    source.src,
                                    imageView?.width,
                                    imageView?.height
                                ).also { bm ->
                                    imageCache?.addAndGet(key, bm)
                                    imageView?.setImageBitmap(bm)
                                    onBitmapLoaded?.invoke(bm)
                                }
                            }
                        }
                    }
                }
            }
        )
    }

    /**
     * Initializing scope if it is canceled and create cache for first time access.
     * */
    fun init(context: Context) = Config().also {
        if (scope.coroutineContext[Job]?.isCancelled == true) scope =
            CoroutineScope(Dispatchers.Main)
        synchronized(this) {
            if (!areCacheInitialized) {
                mixCache = BilderCache(
                    InMemoryCache().also {
                        this.diskCache = it
                    },
                    DiskCache(context).also {
                        this.inMemoryCache = it
                    }
                )
                areCacheInitialized = true
            }
        }
    }

    /**
     * Key used for caching bitmaps in [BilderCache].
     * */
    private fun geKey(source: Source) = when (source) {
        is Source.Url -> source.src.replace('/', '_').replace(':', '_')
        is Source.Bitmap -> source.src.toString()
        is Source.DrawableRes -> source.src.toString()
    }

    /**
     * Add [View.addOnAttachStateChangeListener] on the imageView. This is to cancel any redundant
     * coroutine for views that have been detached.
     * */
    private fun prepareImageView(
        imageView: ImageView?,
        scope: CoroutineScope,
    ) {
        imageView?.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(p0: View?) {
            }

            override fun onViewDetachedFromWindow(p0: View?) {
                scope.cancel()
            }
        })
    }

    /**
     * Cancel current [scope].
     * */
    fun stop() {
        scope.cancel()
    }

    class Task(private val job: Job) {
        fun cancel() = job.cancel()
    }
}

fun log(message: String) {
    Log.d(TAG_DEBUG, message)
}
