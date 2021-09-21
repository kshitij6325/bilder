package com.example.bilder

sealed class Source {
    class Bitmap(val src: android.graphics.Bitmap) : Source()
    class Url(val src: String) : Source()
    class DrawableRes(val src: Int) : Source()
}
