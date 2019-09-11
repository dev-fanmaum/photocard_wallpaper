package com.photocard.wallpaper

import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.PointF
import android.graphics.Rect
import android.support.annotation.RequiresPermission
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.ImageView
import com.photocard.wallpaper.util.WallPaperCallBack
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

    private val scaleListener: ScaleGestureDetector

    private val clickPoint = PointF()
    private val wallpaperHitRect = Rect()
        get() {
            wallPaperImageView.getHitRect(field)
            return field
        }

    init {
        addView(wallPaperImageView)
        scaleListener = ScaleGestureDetector(context, ScaleListener())
    }


    private val minScaleSize = 1f
    private val maxScaleSize = 3f


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
        scaleListener.onTouchEvent(event)

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
                fixPosition(100)
            }
        }

        wallPaperImageView.getHitRect(wallpaperHitRect)

        return true
    }

    private fun fixPosition(duringTime : Long) {
        val nowRect = wallpaperHitRect

        val xPosition = nowRect.left.toFloat()
        val yPosition = nowRect.top.toFloat()

        val viewWidth = nowRect.right - nowRect.left
        val viewHeight = nowRect.bottom - nowRect.top

        val scaleXCurrent = xPosition - wallPaperImageView.x
        val scaleYCurrent = yPosition - wallPaperImageView.y

        val horizontalPosition = when {
            xPosition > deviceViewBox.left -> deviceViewBox.left - scaleXCurrent
            xPosition < (deviceViewBox.right - viewWidth) -> deviceViewBox.right - scaleXCurrent - viewWidth
            else -> xPosition - scaleXCurrent
        }
        val verticalPosition = when {
            yPosition > deviceViewBox.top -> deviceViewBox.top - scaleYCurrent
            yPosition < (deviceViewBox.bottom - viewHeight) -> deviceViewBox.bottom - scaleYCurrent - viewHeight
            else -> yPosition - scaleYCurrent
        }

        wallPaperImageView.animate()
            .x(horizontalPosition)
            .y(verticalPosition)
            .setDuration(duringTime)
            .start()
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector?): Boolean {
            return super.onScaleBegin(detector)
        }

        override fun onScale(detector: ScaleGestureDetector?): Boolean {
            detector ?: return false
            wallPaperImageView.animate()
                .scaleX(wallPaperImageView.scaleX * detector.scaleFactor)
                .scaleY(wallPaperImageView.scaleY * detector.scaleFactor)
                .setDuration(0)
                .start()
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector?) {
            super.onScaleEnd(detector)
            detector?: return
            val viewScaleSize = wallPaperImageView.scaleX

            val transScale = when {
                viewScaleSize < minScaleSize -> minScaleSize
                viewScaleSize > maxScaleSize -> maxScaleSize
                else -> null
            }

            transScale?.run {
                wallPaperImageView.animate()
                    .scaleX(this)
                    .scaleY(this)
                    .x(detector.focusX)
                    .y(detector.focusY)
                    .setDuration(200)
                    .withEndAction { fixPosition(0) }
                    .start()
            }

        }


    }


    @ExperimentalCoroutinesApi
    @RequiresPermission(allOf = [android.Manifest.permission.SET_WALLPAPER, android.Manifest.permission.SET_WALLPAPER_HINTS])
    @Synchronized
    fun saveAndCutBitmap(callback: WallPaperCallBack) {
        if (checkWallPaperProcess) return
        checkWallPaperProcess = true

        val wallpaperRect = wallpaperHitRect

        val transLeft = wallpaperRect.left
        val transTop = wallpaperRect.top

        val bitmap = Bitmap.createBitmap(
            wallPaperImageView.drawable.intrinsicWidth,
            wallPaperImageView.drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )

        val viewWidth = wallpaperRect.right - wallpaperRect.left
        val viewHeight = wallpaperRect.bottom - wallpaperRect.top

        val flow = flowOf(bitmap)
            .map(::drawBitmap)
            .map { resizeBitmap(it, viewWidth, viewHeight) }
            .map { cropSizeBitmap(it, transLeft, transTop) }
            .catch { bitmapToMapReferenceErrorCatch(it, callback) }
            .map(::userDeviceResize)

        CoroutineScope(Dispatchers.Default).launch { flow.collect { setWallPaper(it, callback) } }

    }

    private suspend fun setWallPaper(
        bitmap: Bitmap,
        callback: WallPaperCallBack
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

    private suspend fun resizeBitmap(bitmap: Bitmap, width: Int, height: Int): Bitmap =
        Bitmap.createScaledBitmap(
            bitmap,
            width,
            height,
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
        callback: WallPaperCallBack
    ) {
        withContext(Dispatchers.Main) { callback.error(e) }
        checkWallPaperProcess = false
        e.printStackTrace()
    }

    private suspend fun userDeviceResize(bitmap: Bitmap): Bitmap =
        Bitmap.createScaledBitmap(bitmap, userDeviceWidth.toInt(), userDeviceHeight.toInt(), true)

}