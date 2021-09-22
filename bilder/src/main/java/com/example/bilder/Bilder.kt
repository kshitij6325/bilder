package com.example.bilder

import android.app.Activity
import android.graphics.Bitmap
import android.util.Log
import android.util.LruCache
import android.view.View
import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.example.bilder.cache.*
import com.example.bilder.cache.Cache
import com.example.bilder.cache.DiskCache
import com.example.bilder.cache.InMemoryCache
import com.example.bilder.cache.NoCache
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
     * For caching bitmaps. Currently only supports inMemory caching using [LruCache]
     * */
    private var imageCache: Cache<Bitmap?, Bitmap?>? = null

    /**
     * Loads image from url to imageView. If image is available in cache use that else download the
     * image,cache it and then use the scaled image for imageView
     *
     * @param activity for which the request is initiated.Required to handle cancellation if [Activity.isFinishing]
     * @param imageUrl Url of the image to be loaded.
     * @param placeHolder drawable id for placeholder image
     * @param onBitmapLoaded callback for bitmap successfully loaded
     * @param onBitmapLoadFailure callback for when bitmap load fails
     *
     * @return [Task] for the current request
     * */

    class Config(private val activity: Activity) {

        var disableMemoryCache: Boolean = false
        var disableDiskCache: Boolean = false

        var onBitmapLoaded: ((Bitmap) -> Unit)? = null
        var onBitmapLoadFailure: ((java.lang.Exception) -> Unit)? = null

        fun configure(configBuilder: Config.() -> Unit) = apply(configBuilder)

        fun load(
            source: Source,
            imageView: ImageView? = null,
            @DrawableRes
            placeHolder: Int? = null
        ) = Task(
            kotlin.run {
                synchronized(this) {
                    if (imageCache == null) {
                        imageCache = when {
                            disableDiskCache && disableMemoryCache -> NoCache()
                            disableMemoryCache && !disableDiskCache -> DiskCache(activity)
                            !disableDiskCache && disableMemoryCache -> InMemoryCache()
                            else -> BilderCache(activity)
                        }
                    }
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
                                imageDownloader.download(source.src, activity).run {
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
                                    activity.resources,
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

    fun init(activity: Activity) = Config(activity).also {
        if (scope.coroutineContext[Job]?.isCancelled == true) scope =
            CoroutineScope(Dispatchers.Main)
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
     * Set placeholder drawable.
     * Add [View.addOnAttachStateChangeListener] on the imageView. This is to cancel any redundant
     * coroutine for views that have been detached.
     * */
    private fun prepareImageView(
        imageView: ImageView?,
        scope: CoroutineScope,
    ) {
        imageView?.run {
            addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
                override fun onViewAttachedToWindow(p0: View?) {
                }

                override fun onViewDetachedFromWindow(p0: View?) {
                    scope.cancel()
                }
            })
        }
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
