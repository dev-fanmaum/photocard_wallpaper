package com.photocard.wallpaper

import android.annotation.SuppressLint
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
import com.photocard.wallpaper.tast.settingBitmap
import com.photocard.wallpaper.util.WallPaperCallBack
import kotlin.math.max

class WallPaperView @JvmOverloads constructor(
    context: Context,
    attributes: AttributeSet? = null,
    defStyleAtt: Int = 0
) : OverlayViewLayout(context, attributes, defStyleAtt) {


    @Volatile
    private var motionState = MotionEvent.CLASSIFICATION_NONE
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


    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return false
        scaleListener.onTouchEvent(event)

        val curr = PointF(event.x, event.y)

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                motionState = MotionEvent.ACTION_DOWN
                clickPoint.set(wallPaperImageView.x - curr.x, wallPaperImageView.y - curr.y)
            }

            MotionEvent.ACTION_MOVE -> {
                if (motionState == MotionEvent.ACTION_DOWN)
                    wallPaperImageView.animate()
                        .x(curr.x + clickPoint.x)
                        .y(curr.y + clickPoint.y)
                        .setDuration(0L)
                        .start()
            }

            MotionEvent.ACTION_UP -> {
                if (motionState == MotionEvent.ACTION_DOWN)
                    fixPosition(100)
            }
        }

        wallPaperImageView.getHitRect(wallpaperHitRect)

        return true
    }

    @Suppress("SameParameterValue")
    private fun fixPosition(duringTime: Long = 0) {
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
            motionState = MotionEvent.ACTION_POINTER_DOWN
            detector ?: return false
            clickPoint.set(
                wallPaperImageView.x - detector.focusX,
                wallPaperImageView.y - detector.focusY
            )
            return super.onScaleBegin(detector)
        }

        override fun onScale(detector: ScaleGestureDetector?): Boolean {
            detector ?: return false
            if (motionState == MotionEvent.ACTION_POINTER_DOWN)
                wallPaperImageView.animate()
                    .scaleX(wallPaperImageView.scaleX * detector.scaleFactor)
                    .scaleY(wallPaperImageView.scaleY * detector.scaleFactor)
                    .x(clickPoint.x + detector.focusX)
                    .y(clickPoint.y + detector.focusY)
                    .setDuration(0)
                    .start()
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector?) {
            super.onScaleEnd(detector)
            motionState = MotionEvent.CLASSIFICATION_NONE
            detector ?: return
            val viewScaleSize = wallPaperImageView.scaleX

            val transScale = when {
                viewScaleSize < minScaleSize -> minScaleSize
                viewScaleSize > maxScaleSize -> maxScaleSize
                else -> null
            }

            transScale?.run {
                wallPaperImageView.animate()
                    .withStartAction { fixPosition(100) }
                    .scaleX(this)
                    .scaleY(this)
                    .setDuration(100)
                    .withEndAction { fixPosition(100) }
                    .start()
            }

        }


    }


    @RequiresPermission(allOf = [android.Manifest.permission.SET_WALLPAPER, android.Manifest.permission.SET_WALLPAPER_HINTS])
    @Synchronized
    fun saveAndCutBitmap(callback: WallPaperCallBack) {
        val bitmap = Bitmap.createBitmap(
            wallPaperImageView.drawable.intrinsicWidth,
            wallPaperImageView.drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        wallPaperImageView.drawable.draw(canvas)

        val wallpaperRect = wallpaperHitRect
        val viewWidth = wallpaperRect.width()
        val viewHeight = wallpaperRect.height()

        val rect = Rect(
            (deviceViewBox.left - wallpaperRect.left).toInt(),
            (deviceViewBox.top - wallpaperRect.top).toInt(),
            deviceViewWidth.toInt(),
            deviceViewHeight.toInt()
        )

        settingBitmap(
            context,
            bitmap,
            viewWidth, viewHeight,
            userDeviceWidth.toInt(), userDeviceHeight.toInt(),
            rect,
            callback
        )
    }


}