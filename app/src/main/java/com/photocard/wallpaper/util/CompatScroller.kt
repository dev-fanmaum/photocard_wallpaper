package com.photocard.wallpaper.util

import android.annotation.TargetApi
import android.content.Context
import android.os.Build
import android.widget.OverScroller
import android.widget.Scroller


@TargetApi(Build.VERSION_CODES.GINGERBREAD)
internal class CompatScroller(context: Context) {
    private lateinit var scroller: Scroller
    private lateinit var overScroller: OverScroller
    private var isPreGingerbread: Boolean = false

    val isFinished: Boolean
        get() = if (isPreGingerbread) scroller.isFinished else overScroller.isFinished

    val currX: Int
        get() = if (isPreGingerbread) scroller.currX else overScroller.currX

    val currY: Int
        get() = if (isPreGingerbread) scroller.currY else overScroller.currY

    init {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.GINGERBREAD) {
            isPreGingerbread = true
            scroller = Scroller(context)

        } else {
            isPreGingerbread = false
            overScroller = OverScroller(context)
        }
    }

    fun fling(startX: Int, startY: Int, velocityX: Int, velocityY: Int, minX: Int, maxX: Int, minY: Int, maxY: Int) =
        if (isPreGingerbread)
            scroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY)
        else
            overScroller.fling(startX, startY, velocityX, velocityY, minX, maxX, minY, maxY)

    fun forceFinished(finished: Boolean) =
        if (isPreGingerbread) scroller.forceFinished(finished) else overScroller.forceFinished(finished)

    fun computeScrollOffset(): Boolean =
        if (isPreGingerbread) {
            scroller.computeScrollOffset()
        } else {
            overScroller.computeScrollOffset()
            overScroller.computeScrollOffset()
        }
}