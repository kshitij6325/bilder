package com.example.imageloadingtest

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class ButtonActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_button)

        findViewById<Button>(R.id.btn).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}
