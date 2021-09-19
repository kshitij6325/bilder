package com.example.bilder2

import android.app.Activity
import android.graphics.Bitmap
import android.util.Log
import android.util.LruCache
import android.view.View
import android.widget.ImageView
import androidx.annotation.DrawableRes
import com.example.bilder2.cache.BilderCache
import com.example.bilder2.cache.Cache
import com.example.bilder2.network.BilderDownloader
import com.example.bilder2.network.DownloadRequest
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
    private var imageCache: Cache<ByteArray, Bitmap?>? = null

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
        imageUrl: String,
        imageView: ImageView? = null,
        @DrawableRes placeHolder: Int? = null,
        onBitmapLoaded: ((Bitmap) -> Unit)? = null,
        onBitmapLoadFailure: ((java.lang.Exception) -> Unit)? = null
    ) = Task(
        kotlin.run {
            synchronized(this) {
                if (imageCache == null) {
                    imageCache = BilderCache.init(context = activity)
                }
            }
            imageView?.setImageResource(placeHolder ?: 0)
            scope.launch {
                val key = geKey(imageUrl)
                prepareImageView(imageView, this)
                imageCache?.get(key)?.run {
                    onBitmapLoaded?.invoke(this)
                    scaleAndSetBitmap(this, imageView = imageView)
                } ?: run {
                    imageDownloader.download(imageUrl, activity).run {
                        when (this) {
                            is DownloadRequest.Success -> {
                                imageCache?.addAndGet(key, data)
                                    ?.run {
                                        onBitmapLoaded?.invoke(this)
                                        scaleAndSetBitmap(this, imageView = imageView)
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
            }
        }
    )

    /**
     * Key used for caching bitmaps in [BilderCache].
     * */
    private fun geKey(imageUrl: String) = imageUrl.replace('/', '_').replace(':', '_')

    /**
     * Scale and set bitmap according to imageView dimensions.
     * */
    private suspend fun scaleAndSetBitmap(bitmap: Bitmap, imageView: ImageView?): Bitmap? =
        withContext(Dispatchers.Default) {
            if (isActive) imageView?.let {
                val (height: Int, width: Int) = getRequiredSize(bitmap, imageView)
                log("${bitmap.height},${bitmap.width} is scaled to $height,$width")
                Bitmap.createScaledBitmap(bitmap, width, height, false).also {
                    withContext(Dispatchers.Main) {
                        log((bitmap === it).toString())
                        imageView.setImageBitmap(it)
                    }
                }
            } else throw CancellationException()
        }

    /**
     * Returns the required dimensions in [scaleAndSetBitmap] that the bitmap needs to scale to.
     * */
    private fun getRequiredSize(bitmap: Bitmap, imageView: ImageView): Pair<Int, Int> {
        val (bmHeight: Float, bmWidth: Float) = bitmap.height.toFloat() to bitmap.width.toFloat()
        val (imHeightLp: Int, imWidthLp: Int) = imageView.height to imageView.width
        return if (imHeightLp > 0 && imWidthLp > 0 && (imHeightLp < bmHeight || imWidthLp < bmWidth)) {
            if (imWidthLp > imHeightLp && bmWidth > imWidthLp) {
                ((bmHeight / bmWidth) * imWidthLp).toInt() to imWidthLp
            } else
                imHeightLp to ((bmHeight / bmWidth) * imHeightLp).toInt()
        } else bmHeight.toInt() to bmWidth.toInt()
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

    class Task(private val job: Job) {
        fun cancel() = job.cancel()
    }
}

fun log(message: String) {
    Log.d(TAG_DEBUG, message)
}
