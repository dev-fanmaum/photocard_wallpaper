package com.photocard.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PointF
import android.support.annotation.RequiresPermission
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.widget.ImageView
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlin.math.max

class WallPaperView @JvmOverloads constructor(
    context: Context,
    attributes: AttributeSet? = null,
    defStyleAtt: Int = 0
) : OverlayViewLayout(context, attributes, defStyleAtt) {


    @Volatile
    private var checkWallPaperProcess = false
    private val wallPaperImageView = WallpaperImageView(context)

    private val clickPoint = PointF()

    init {
        addView(wallPaperImageView)
    }

    fun getWallPaperImageView(): ImageView = wallPaperImageView

    override fun deviceSizeSetting(viewWidth: Int, viewHeight: Int) {
        super.deviceSizeSetting(viewWidth, viewHeight)
        wallPaperImageView.setChangeEvent { width, height ->
            wallPaperImageView.layoutParams = innerBoxImageViewSetting(width, height)
        }
    }

    private fun innerBoxImageViewSetting(width: Int, height: Int): LayoutParams {
        val viewRatioSize = max(
            (deviceViewBox.right - deviceViewBox.left) / width,
            (deviceViewBox.bottom - deviceViewBox.top) / height
        )
        return LayoutParams(
            (width * viewRatioSize).toInt(),
            (height * viewRatioSize).toInt()
        ).apply {
            gravity = Gravity.CENTER
        }
    }


    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return false

        val curr = PointF(event.x, event.y)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                clickPoint.set(wallPaperImageView.x - curr.x, wallPaperImageView.y - curr.y)
            }

            MotionEvent.ACTION_MOVE -> {
                wallPaperImageView.animate()
                    .x(curr.x + clickPoint.x)
                    .y(curr.y + clickPoint.y)
                    .setDuration(0L)
                    .start()
            }

            MotionEvent.ACTION_UP -> {
                fixPosition()
            }
        }

        return true
    }

    private fun fixPosition() {
        val horizontalPosition = when {
            wallPaperImageView.x > deviceViewBox.left -> deviceViewBox.left
            wallPaperImageView.x < (deviceViewBox.right - wallPaperImageView.width) -> deviceViewBox.right - wallPaperImageView.width
            else -> wallPaperImageView.x
        }
        val verticalPosition = when {
            wallPaperImageView.y > deviceViewBox.top -> deviceViewBox.top
            wallPaperImageView.y < (deviceViewBox.bottom - wallPaperImageView.height) -> deviceViewBox.bottom - wallPaperImageView.height
            else -> wallPaperImageView.y
        }

        wallPaperImageView.animate()
            .x(horizontalPosition)
            .y(verticalPosition)
            .setDuration(100)
            .start()
    }


    @ExperimentalCoroutinesApi
    @RequiresPermission(allOf = [android.Manifest.permission.SET_WALLPAPER, android.Manifest.permission.SET_WALLPAPER_HINTS])
    @Synchronized
    fun saveAndCutBitmap(callback: WallPaperSupportImageView.WallPaperCallBack) {
        if (checkWallPaperProcess) return
        checkWallPaperProcess = true

        val transLeft = wallPaperImageView.x
        val transTop = wallPaperImageView.y

        val bitmap = Bitmap.createBitmap(
            wallPaperImageView.drawable.intrinsicWidth,
            wallPaperImageView.drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )

        val flow = flowOf(bitmap)
            .map(::drawBitmap)
            .map(::resizeBitmap)
            .map { cropSizeBitmap(it, transLeft.toInt(), transTop.toInt()) }
            .catch { bitmapToMapReferenceErrorCatch(it, callback) }
            .map(::userDeviceResize)

        CoroutineScope(Dispatchers.Default).launch { flow.collect { setWallPaper(it, callback) } }

    }

    private suspend fun setWallPaper(
        bitmap: Bitmap,
        callback: WallPaperSupportImageView.WallPaperCallBack
    ) {
        val wallPaperManager = WallpaperManager.getInstance(context)
        wallPaperManager.setBitmap(bitmap)

        checkWallPaperProcess = false
        withContext(Dispatchers.Main) { callback.complete() }

    }

    private suspend fun drawBitmap(bitmap: Bitmap): Bitmap {
        val canvas = Canvas(bitmap)
        wallPaperImageView.drawable.draw(canvas)
        return bitmap
    }

    private suspend fun resizeBitmap(bitmap: Bitmap): Bitmap = Bitmap.createScaledBitmap(
        bitmap,
        wallPaperImageView.width,
        wallPaperImageView.height,
        true
    )

    private suspend fun cropSizeBitmap(bitmap: Bitmap, left: Int, top: Int): Bitmap =
        Bitmap.createBitmap(
            bitmap,
            (deviceViewBox.left - left).toInt(),
            (deviceViewBox.top - top).toInt(),
            deviceViewWidth.toInt(),
            deviceViewHeight.toInt()
        )

    private suspend fun bitmapToMapReferenceErrorCatch(
        e: Throwable,
        callback: WallPaperSupportImageView.WallPaperCallBack
    ) {
        withContext(Dispatchers.Main) { callback.error(e) }
        checkWallPaperProcess = false
        e.printStackTrace()
    }

    private suspend fun userDeviceResize(bitmap: Bitmap): Bitmap =
        Bitmap.createScaledBitmap(bitmap, userDeviceWidth.toInt(), userDeviceHeight.toInt(), true)

}