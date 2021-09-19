package com.example.bilder2.network

import android.app.Activity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.net.URL

/**
 * Downloads image from given url on [Dispatchers.IO] thread.
 * Cancels request if given [Activity] is in finishing state.
 * */
internal object BilderDownloader {

    suspend fun download(imageUrl: String, activity: Activity): DownloadRequest =
        withContext(Dispatchers.IO) {
            try {
                val url = URL(imageUrl)
                val bufferedInputStream = BufferedInputStream(url.openStream())
                val fileOutputStream = ByteArrayOutputStream()
                val byteArr = ByteArray(1024)
                var len = bufferedInputStream.read(byteArr)
                while (len != -1 && isActive) {
                    if (activity.isFinishing) {
                        cancel()
                    }
                    fileOutputStream.write(byteArr, 0, len)
                    len = bufferedInputStream.read(byteArr)
                }
                fileOutputStream.close()
                bufferedInputStream.close()
                if (isActive) {
                    return@withContext DownloadRequest.Success(
                        fileOutputStream.toByteArray()
                    )
                } else {
                    return@withContext DownloadRequest.Canceled
                }
            } catch (e: Exception) {
                return@withContext DownloadRequest.Error(e)
            }
        }
}

internal sealed class DownloadRequest {
    class Success(val data: ByteArray) : DownloadRequest()
    class Error(val e: Exception) : DownloadRequest()
    object Canceled : DownloadRequest()
}
