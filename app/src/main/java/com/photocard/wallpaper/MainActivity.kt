package com.photocard.wallpaper

import android.app.Activity
import android.os.Bundle
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : Activity() {

    private val imageUri =
        "http://cdnetphoto.appphotocard.com/289/26/HASH_56f18de249439339c4e345349eaee4ced68e8bdd76acb4153f7a79d8c49d75a2.png"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        img.setImageGlide(
            Glide.with(this)
                .load(imageUri)
        )

        button1.setOnClickListener {
            img.saveAndCutBitmap()
        }


    }
}
