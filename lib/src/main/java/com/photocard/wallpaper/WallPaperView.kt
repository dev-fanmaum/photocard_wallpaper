package com.photocard.wallpaper

import android.content.Context
import android.graphics.PointF
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.widget.ImageView
import kotlin.math.max

class WallPaperView @JvmOverloads constructor(
    context: Context,
    attributes: AttributeSet? = null,
    defStyleAtt: Int = 0
) : OverlayViewLayout(context, attributes, defStyleAtt) {

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
            wallPaperImageView.x < (deviceViewBox.right - wallPaperImageView.width)  -> deviceViewBox.right - wallPaperImageView.width
            else -> wallPaperImageView.x
        }
        val verticalPosition = when {
            wallPaperImageView.y > deviceViewBox.top -> deviceViewBox.top
            wallPaperImageView.y < (deviceViewBox.bottom - wallPaperImageView.height)  -> deviceViewBox.bottom - wallPaperImageView.height
            else -> wallPaperImageView.y
        }

        wallPaperImageView.animate()
            .x(horizontalPosition)
            .y(verticalPosition)
            .setDuration(100)
            .start()

    }


}