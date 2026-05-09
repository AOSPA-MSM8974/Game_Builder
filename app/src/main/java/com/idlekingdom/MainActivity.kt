package com.idlekingdom

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val btn = Button(this)
        btn.text = "PLAY"

        btn.setOnClickListener {
            startActivity(Intent(this, GameActivity::class.java))
        }

        setContentView(btn)
    }
}
