package com.photocard.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.util.AttributeSet
import androidx.annotation.RequiresPermission
import java.util.jar.Manifest

class WallPaperSupportImageView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttribute: Int = 0
) : TouchImageView(context, attributeSet, defStyleAttribute) {


    @RequiresPermission(android.Manifest.permission.SET_WALLPAPER)
    fun saveAndCutBitmap() {

        val viewTopTrans = m[Matrix.MTRANS_X]
        val viewLeftTrans = m[Matrix.MTRANS_X]


       /* val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )

//        BitmapFactory.decodeStream(drawable.)

        val canvas = Canvas(bitmap)


        drawable.run {
            setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
            draw(canvas)
        }

        WallpaperManager.getInstance(context)
            .setBitmap(bitmap)*/


    }

}