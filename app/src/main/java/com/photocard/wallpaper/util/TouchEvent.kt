package com.photocard.wallpaper.util

import android.graphics.PointF

interface TouchEvent {

    fun actionDown(curr : PointF)
    fun actionMove(curr : PointF)
    fun actionUp(curr : PointF)
    fun actionPointerUp(curr : PointF)
}