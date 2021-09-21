package com.example.imageloadingtest

import android.app.Activity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.bilder.Bilder
import com.example.bilder.Source

class ImageAdapter(private val list: List<Pair<Int, String>>) :
    RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    class ImageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val image = view.findViewById<ImageView>(R.id.image)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        return ImageViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.layout_image, parent, false)
        )
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        Bilder.load(
            holder.image.context as Activity,
            source = Source.DrawableRes(R.drawable.placeholder),
            onBitmapLoadFailure = {
                Log.e("Bilder log", "failed with ${it.message}")
            },
            onBitmapLoaded = {
                Log.e("Bilder log", "Successfully loaded")
            }
        )
    }

    override fun getItemCount() = list.size
}
