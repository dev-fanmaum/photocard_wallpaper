package com.photocard.wallpaper

import android.app.Activity
import android.os.Bundle
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : Activity() {

    private val imageUri =
        "http://cdnetphoto.appphotocard.com/289/21/HASH_3f191f1bf72d46f023b97d7423292aca25d6a7732556e92f313ce07716bb9c34.png"

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
