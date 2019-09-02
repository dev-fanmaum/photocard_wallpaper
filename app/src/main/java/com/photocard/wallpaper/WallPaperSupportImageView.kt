package com.photocard.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.util.AttributeSet
import androidx.annotation.RequiresPermission
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.abs
import kotlin.math.min

class WallPaperSupportImageView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttribute: Int = 0
) : TouchImageView(context, attributeSet, defStyleAttribute) {

    interface WallPaperCallBack {
        fun error(e: Throwable)
        fun complete()
    }

    private var callback: WallPaperCallBack? = null

    @Volatile
    private var checkWallPaperProcess = false

    private val viewLeftTrans get() = m[Matrix.MTRANS_X].toInt()
    private val viewTopTrans get() = m[Matrix.MTRANS_Y].toInt()

    private val deviceSizeFromOverlayToWidthSize get() = (deviceForegroundBoxSize.right - deviceForegroundBoxSize.left).toInt()
    private val deviceSizeFromOverlayToHeightSize get() = (deviceForegroundBoxSize.bottom - deviceForegroundBoxSize.top).toInt()

    @ExperimentalCoroutinesApi
    @RequiresPermission(android.Manifest.permission.SET_WALLPAPER)
    @Synchronized
    fun saveAndCutBitmap() {
        if (checkWallPaperProcess) return
        checkWallPaperProcess = true

        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )

        val flow = flowOf(bitmap)
            .flowOn(Dispatchers.Default)
            .map(::resizeBitmap)
            .map(::cropSizeBitmap)
            .catch { bitmapToMapReferenceErrorCatch(it) }
            .map(::userDeviceResize)

        CoroutineScope(Dispatchers.Default).launch { flow.collect { setWallPaper(it) } }

    }

    private suspend fun setWallPaper(bitmap: Bitmap) {
        WallpaperManager.getInstance(context).setBitmap(bitmap)
        checkWallPaperProcess = false
        withContext(Dispatchers.Main) { callback?.complete() }

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

    private suspend fun bitmapToMapReferenceErrorCatch(e: Throwable) {
        withContext(Dispatchers.Main) { callback?.error(e) }
        checkWallPaperProcess = false
        e.printStackTrace()
    }

    private suspend fun userDeviceResize(bitmap: Bitmap): Bitmap =
        Bitmap.createScaledBitmap(bitmap, deviceWidth.toInt(), deviceHeight.toInt(), true)

    fun setWallPaperCallBack(callBack: WallPaperCallBack) {
        this.callback = callBack
    }

}

