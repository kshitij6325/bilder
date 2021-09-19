package com.example.imageloadingtest

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.example.imageloadingtest.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private var binding: ActivityMainBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        binding = ActivityMainBinding.inflate(layoutInflater)
        super.onCreate(savedInstanceState)
        setContentView(binding?.root)
        binding?.let {
            it.recyclerView.adapter = ImageAdapter(getImageList())
            it.recyclerView.layoutManager =
                GridLayoutManager(this, 4)
        }
    }

    private fun getImageList() = (0..2000).map {
        it to "https://picsum.photos/id/$it/200/400"
    }
}
