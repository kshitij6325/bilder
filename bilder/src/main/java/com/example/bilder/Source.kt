package com.example.bilder

import android.graphics.Bitmap

sealed class Source {
    class Bitmap(val src: android.graphics.Bitmap) : Source()
    class Url(val src: String) : Source()
    class DrawableRes(val src: Int) : Source()
}
