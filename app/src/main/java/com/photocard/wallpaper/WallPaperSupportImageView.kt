package com.photocard.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.util.AttributeSet
import android.util.Log
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.min

class WallPaperSupportImageView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttribute: Int = 0
) : TouchImageView(context, attributeSet, defStyleAttribute) {

    @Volatile
    private var checkWallPaperProcess = false

    private val viewLeftTrans get() = m[Matrix.MTRANS_X].toInt()
    private val viewTopTrans get() = m[Matrix.MTRANS_Y].toInt()

    private val deviceSizeFromOverlayToWidthSize get() = (deviceForegroundBoxSize.right - deviceForegroundBoxSize.left).toInt()
    private val deviceSizeFromOverlayToHeightSize get() = (deviceForegroundBoxSize.bottom - deviceForegroundBoxSize.top).toInt()

    @RequiresPermission(android.Manifest.permission.SET_WALLPAPER)
    @Synchronized
    fun saveAndCutBitmap() {

        CoroutineScope(Dispatchers.Default).launch {
            if (checkWallPaperProcess) cancel()
            checkWallPaperProcess = true

            val bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )

            flowOf(bitmap)
                .map(::resizeBitmap)
                .map(::cropSizeBitmap)
                .catch {
                    checkWallPaperProcess = false
                    it.printStackTrace()
                }
                .flowOn(Dispatchers.Main)
                .collect { setWallPaper(it) }

        }
    }

    private fun setWallPaper(bitmap: Bitmap) {
        WallpaperManager.getInstance(context).setBitmap(bitmap)
        checkWallPaperProcess = false

        Log.i("wallPaper", "Setting Complete")
    }

    private suspend fun resizeBitmap(bitmap: Bitmap): Bitmap {
        val canvas = Canvas(bitmap)
        drawable.draw(canvas)

        return Bitmap.createScaledBitmap(
            bitmap,
            getImageWidth().toInt(),
            getImageHeight().toInt(),
            true
        )
    }

    private suspend fun cropSizeBitmap(bitmap: Bitmap): Bitmap {
        val xSize = abs(viewLeftTrans - deviceForegroundBoxSize.left.toInt())
        val ySize = abs(viewTopTrans - deviceForegroundBoxSize.top.toInt())

        return Bitmap.createBitmap(
            bitmap,
            xSize,
            ySize,
            min((getImageWidth() - xSize).toInt(), deviceSizeFromOverlayToWidthSize),
            min((getImageHeight() - ySize).toInt(), deviceSizeFromOverlayToHeightSize)
        )
    }

}