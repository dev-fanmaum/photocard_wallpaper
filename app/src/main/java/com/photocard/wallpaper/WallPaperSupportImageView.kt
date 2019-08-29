package com.photocard.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
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

            val viewLeftTrans = abs(m[Matrix.MTRANS_X].toInt())
            val viewTopTrans = abs(m[Matrix.MTRANS_Y].toInt())

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
                Bitmap.createScaledBitmap(
                    bitmap,
                    getImageWidth().toInt(),
                    getImageHeight().toInt(),
                    true
                )


            Log.i(
                "BitmapSizePrint", """
            |
            |Width = ${drawable.intrinsicWidth},
            |Height = ${drawable.intrinsicHeight}
            |reSizeBitmap Width = ${getImageWidth().toInt()},
            |reSizeBitmap Height = ${getImageHeight().toInt()}
            |LEFT = ${viewLeftTrans + deviceForegroundBoxSize.left.toInt()},
            |TOP = ${viewTopTrans + deviceForegroundBoxSize.top.toInt()},
            |RIGHT = ${viewLeftTrans + deviceForegroundBoxSize.right.toInt()},
            |BOTTOM = ${viewTopTrans + deviceForegroundBoxSize.bottom.toInt()}
            |viewLeftTrans = $viewLeftTrans
            |viewTopTrans = $viewTopTrans"""
            )
            val cropBitmap = Bitmap.createBitmap(
                resizeBitmap,
                viewLeftTrans + deviceForegroundBoxSize.left.toInt(),
                viewTopTrans + deviceForegroundBoxSize.top.toInt(),
                viewLeftTrans + deviceForegroundBoxSize.right.toInt(),
                viewTopTrans + deviceForegroundBoxSize.bottom.toInt()
            )

            withContext(Dispatchers.Main) {
                WallpaperManager.getInstance(context).setBitmap(cropBitmap)
            }

        }


    }

}