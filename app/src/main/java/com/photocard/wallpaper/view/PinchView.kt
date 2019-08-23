package com.photocard.wallpaper.view

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.widget.ImageView

class PinchView @JvmOverloads constructor(
    context: Context, attribute: AttributeSet? = null, defStyleAttr: Int = 0
) :
    ImageView(context, attribute, defStyleAttr) {

    private val scaleListener = ScaleListener()
    private val gestureListener = GestureListener()

    private var scaleEnable = true

    fun setScaleEnable(enable: Boolean) {
        scaleEnable = enable
    }

    private val scaleDetector = ScaleGestureDetector(context, scaleListener)
    private val gestureDetector = ScaleGestureDetector(context, scaleListener)


    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event == null) return false
        if (event.action eventCheck MotionEvent.ACTION_DOWN) {
            cancelAllAnimation()
        }

        if (event.pointerCount > 1) {
            event.getX(0) + event.getY(1) / 2
            event.getX(1) + event.getY(0) / 2
        }

        gestureDetector.onTouchEvent(event)

        if (scaleEnable) scaleDetector.onTouchEvent(event)

        if ( event.action eventCheck MotionEvent.ACTION_UP){
            setImageToWrapCropBounds()
        }

        return true
    }


    private infix fun Int.eventCheck(eventAction: Int): Boolean = (this and MotionEvent.ACTION_MASK) == eventAction

    private fun cancelAllAnimation() {
        // TODO : Animation Cancel
    }

    private fun setImageToWrapCropBounds() {
        // TODO : CropBounds Animation
    }


    private class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {

        private var mScaleFactor = 1.0f

        override fun onScale(detector: ScaleGestureDetector?): Boolean {
            if (detector == null) return false

            return true
        }
    }

    private class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent?): Boolean {
            // TODO : Zoom In & Zoom Out
            return super.onDoubleTap(e)
        }

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
            // TODO : Translate #distnaceX, #distanceY
            return true
        }
    }

}