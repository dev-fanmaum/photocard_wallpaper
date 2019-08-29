package com.photocard.wallpaper

import android.app.Activity
import android.os.Bundle
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : Activity() {

    private val imageUri =
        "https://search.pstatic.net/sunny/?src=https%3A%2F%2Fcdn.eyeem.com%2Fthumb%2Fac369b97a1438d667014f8a329da431b0364c240-1469397546815%2F1280%2F1280&type=b400"
    private val typeToImage = "image/*"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        img.setImageGlide(
            Glide.with(this)
                .load(imageUri)
                .centerCrop()
        )

        img.saveAndCutBitmap()

        /*button1.setOnClickListener {

            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).apply {
                putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT, ComponentName(this@MainActivity, WallpaperTestService::class.java))
//                component = ComponentName(this@MainActivity, WallpaperTestService::class.java)
            }
            startActivity(intent)

        }*/



    }
}
