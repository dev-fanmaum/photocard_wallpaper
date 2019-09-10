package com.photocard.wallpaper

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.ImageView
import kotlin.math.max

class WallPaperView @JvmOverloads constructor(
    context: Context,
    attributes: AttributeSet? = null,
    defStyleAtt: Int = 0
) : OverlayViewLayout(context, attributes, defStyleAtt) {

    private val wallPaperImageView = WallpaperImageView(context)

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


}