package com.photocard.wallpaper

import android.app.Activity
import android.content.Context
import android.graphics.Point
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.bumptech.glide.Glide
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : Activity(), WallPaperSupportImageView.WallPaperCallBack {

    private val imageUri =
        "http://cdnetphoto.appphotocard.com/289/21/HASH_3f191f1bf72d46f023b97d7423292aca25d6a7732556e92f313ce07716bb9c34.png"
//        "http://cdnetphoto.appphotocard.com/289/21/HASH_51c0e51bf4a0c0e3df24c858f2e26288cbc626609c172084349deb808aa20811.jpg"
//        "http://cdnetphoto.appphotocard.com/289/21/HASH_2a51b783bb6a53606aa6bffd9bd9a1b851d95369e7d1cc0d36bb92b8a2701dfe.png"
//        "http://cdnetphoto.appphotocard.com/289/21/HASH_433c62ea5ed0c9bdfbd702d55e59236517ad31dd71dbd21874b9915ecefd9709.png"

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        img.setImageGlide(
            Glide.with(this)
                .load(imageUri)
        )

        button1.setOnClickListener {
            img.saveAndCutBitmap(this)
        }


        cutoutCheckButton.setOnClickListener {
            val realRect = Point()
            window.decorView?.display?.getRealSize(realRect)
            realRect.run {
                Log.i(
                    "deviceSizeCheck", """rectPrint
                        |x = $x
                        |y = $y
                """.trimMargin()
                )
            }
            window.decorView?.rootWindowInsets?.displayCutout?.let {

                Log.i(
                    "cutOutTest", """CutoutRectPrint
                    |boundingRects = ${it.boundingRects}
                """.trimMargin()
                )
            }
        }

    }

    override fun error(e: Throwable) {
        Toast.makeText(this, "Fail", Toast.LENGTH_SHORT).show()
        e.printStackTrace()
    }

    override fun complete() {
        Toast.makeText(this, "Complete!", Toast.LENGTH_SHORT).show()
    }
}
