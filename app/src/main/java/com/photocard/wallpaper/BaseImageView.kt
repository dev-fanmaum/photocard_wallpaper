package com.photocard.wallpaper

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.graphics.PointF
import android.os.Build
import android.util.AttributeSet
import android.widget.ImageView

abstract class BaseImageView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ImageView(context, attributeSet, defStyleAttr) {


    protected enum class State { NONE, DRAG, ZOOM, FLING, ANIMATE_ZOOM }

    @JvmField
    protected var state: State? = null

    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    protected fun compatPostOnAnimation(runnable: Runnable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) postOnAnimation(runnable)
        else postDelayed(runnable, (1000 / 60).toLong())
    }

    protected abstract fun fixTrans()

    protected abstract fun setState(state : State)

    protected abstract fun actionDown(curr : PointF)
    protected abstract fun actionMove(curr : PointF)
    protected abstract fun actionUp(curr : PointF)
    protected abstract fun actionPointerUp(curr : PointF)

}