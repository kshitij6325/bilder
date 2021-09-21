package com.example.bilder

import android.app.Activity
import android.graphics.Bitmap
import android.util.Log
import android.util.LruCache
import android.view.View
import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.example.bilder.cache.BilderCache
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
    private var scope = CoroutineScope(Dispatchers.Main)

    /**
     * For Downloading images from server.
     * */
    private val imageDownloader = BilderDownloader

    /**
     * For caching bitmaps. Currently only supports inMemory caching using [LruCache]
     * */
    private var imageCache: Cache<Bitmap, Bitmap?>? = null

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
    fun load(
        activity: Activity,
        source: Source,
        imageView: ImageView? = null,
        @DrawableRes placeHolder: Int? = null,
        onBitmapLoaded: ((Bitmap) -> Unit)? = null,
        onBitmapLoadFailure: ((java.lang.Exception) -> Unit)? = null
    ) = Task(
        kotlin.run {
            synchronized(this) {
                if (imageCache == null) {
                    imageCache = BilderCache(context = activity)
                }
            }
            imageView?.setImageResource(placeHolder ?: 0)
            scope.launch {
                val key = geKey(source)
                prepareImageView(imageView, this)
                imageCache?.get(key)?.also {
                    imageView?.run { ->
                        setImageBitmap(it)
                    }
                    onBitmapLoaded?.invoke(it)
                } ?: run {
                    when (source) {
                        is Source.Url -> {
                            imageDownloader.download(source.src, activity).run {
                                when (this) {
                                    is DownloadRequest.Success -> {
                                        imageView?.let {
                                            it.setImageBitmap(
                                                getDownScaledBitmap(data, it.width, it.height)
                                                    .also { bm -> imageCache?.addAndGet(key, bm) }
                                            )
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
                            imageView?.run {
                                setImageBitmap(
                                    getDownScaledBitmap(
                                        source.src,
                                        width,
                                        height
                                    ).also { bm ->
                                        imageCache?.addAndGet(key, bm)
                                    }
                                )
                            }
                        }
                        is Source.DrawableRes -> {
                            log("making new")
                            imageView?.run {
                                setImageBitmap(
                                    getDownScaledBitmap(
                                        context.resources,
                                        source.src,
                                        width,
                                        height
                                    ).also { bm ->
                                        imageCache?.addAndGet(key, bm)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    )

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
     * Cancel all the coroutines running in the [scope].
     * */
    fun stop() {
        scope.cancel()
    }

    class Task(
        private

        val job: Job
    ) {
        fun cancel() = job.cancel()
    }
}

fun log(message: String) {
    Log.d(TAG_DEBUG, message)
}
