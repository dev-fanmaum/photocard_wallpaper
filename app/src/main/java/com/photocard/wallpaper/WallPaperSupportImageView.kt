package com.photocard.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.*
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

            val resizeBitmap = async {
                Bitmap.createScaledBitmap(
                    bitmap,
                    getImageWidth().toInt(),
                    getImageHeight().toInt(),
                    true
                )
            }

            try {
                val cropBitmap = Bitmap.createBitmap(
                    resizeBitmap.await(),
                    viewLeftTrans + deviceForegroundBoxSize.left.toInt(),
                    viewTopTrans + deviceForegroundBoxSize.top.toInt(),
                    deviceForegroundBoxSize.right.toInt(),
                    deviceForegroundBoxSize.bottom.toInt()
                )

                withContext(Dispatchers.Main) {
                    WallpaperManager.getInstance(context).setBitmap(cropBitmap)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

        }


    }

}