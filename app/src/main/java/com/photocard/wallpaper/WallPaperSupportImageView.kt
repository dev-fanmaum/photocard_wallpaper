package com.photocard.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.jar.Manifest
import kotlin.math.abs

class WallPaperSupportImageView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttribute: Int = 0
) : TouchImageView(context, attributeSet, defStyleAttribute) {


    @RequiresPermission(android.Manifest.permission.SET_WALLPAPER)
    fun saveAndCutBitmap() {

        CoroutineScope(Dispatchers.Main).launch {

            val viewTopTrans = abs(m[Matrix.MTRANS_X].toInt())
            val viewLeftTrans = abs(m[Matrix.MTRANS_X].toInt())

            val bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )


            val canvas = Canvas(bitmap)


            drawable.run {
                setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
                draw(canvas)
            }

            val resizeBitmap =
                Bitmap.createScaledBitmap(bitmap, getImageWidth().toInt(), getImageHeight().toInt(), true)

            val cropBitmap = Bitmap.createBitmap(resizeBitmap, viewLeftTrans, viewTopTrans, viewTopTrans + 300, viewLeftTrans+ 300)

            withContext(Dispatchers.Main){

                WallpaperManager.getInstance(context)
//            .setBitmap(Bitmap.createBitmap(bitmap, 100, 100, 300, 300))
                    .setBitmap(cropBitmap)

            }

        }



    }

}