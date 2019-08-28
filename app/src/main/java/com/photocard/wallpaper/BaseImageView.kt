package com.photocard.wallpaper

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.os.Build
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.ImageView
import com.photocard.wallpaper.util.CompatScroller

abstract class BaseImageView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ImageView(context, attributeSet, defStyleAttr) {

    //
    // Scale of image ranges from minScale to maxScale, where minScale == 1
    // when the image is stretched to fit view.
    //
    @JvmField
    protected var normalizedScale: Float = 0.toFloat()

    //
    // Size of view and previous view size (ie before rotation)
    //
    @JvmField
    protected var viewWidth: Int = 0
    @JvmField
    protected var viewHeight: Int = 0
    @JvmField
    protected var prevViewWidth: Int = 0
    @JvmField
    protected var prevViewHeight: Int = 0

    //
    // Size of image when it is stretched to fit view. Before and After rotation.
    //
    @JvmField
    protected var matchViewWidth: Float = 0.toFloat()
    @JvmField
    protected var matchViewHeight: Float = 0.toFloat()
    @JvmField
    protected var prevMatchViewWidth: Float = 0.toFloat()
    @JvmField
    protected var prevMatchViewHeight: Float = 0.toFloat()

    @JvmField
    protected var minScale: Float = .8f
    @JvmField
    protected var maxScale: Float = 3f
    @JvmField
    protected var superMinScale: Float = 0.toFloat()
    @JvmField
    protected var superMaxScale: Float = 0.toFloat()
    @JvmField
    protected var m: FloatArray = FloatArray(9)

    @JvmField
    protected var fling: Fling? = null

    @JvmField
    protected var mScaleDetector: ScaleGestureDetector? = null
    @JvmField
    protected var mGestureDetector: GestureDetector? = null
    @JvmField
    protected var doubleTapListener: GestureDetector.OnDoubleTapListener? = null
    @JvmField
    protected var userTouchListener: OnTouchListener? = null
    @JvmField
    protected var touchImageViewListener: OnTouchImageViewLIstener? = null


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

    protected abstract fun actionDown(curr: PointF)
    protected abstract fun actionMove(curr: PointF)
    protected abstract fun actionUp(curr: PointF)
    protected abstract fun actionPointerUp(curr: PointF)


    /**
     * Fling launches sequential runnables which apply
     * the fling graphic to the image. The values for the translation
     * are interpolated by the Scroller.
     *
     * @author Ortiz
     */
    protected inner class Fling internal constructor(velocityX: Int, velocityY: Int) : Runnable {

        internal var scroller: CompatScroller? = null
        internal var currX: Int = 0
        internal var currY: Int = 0

        init {
            state = State.FLING
            scroller = CompatScroller(context)
            matrix.getValues(m)

            val startX = m[Matrix.MTRANS_X].toInt()
            val startY = m[Matrix.MTRANS_Y].toInt()
            val minX: Int
            val maxX: Int
            val minY: Int
            val maxY: Int

            if (getImageWidth() > viewWidth) {
                minX = viewWidth - getImageWidth().toInt()
                maxX = 0

            } else {
                maxX = startX
                minX = maxX
            }

            if (getImageHeight() > viewHeight) {
                minY = viewHeight - getImageHeight().toInt()
                maxY = 0

            } else {
                maxY = startY
                minY = maxY
            }

            scroller!!.fling(
                startX, startY, velocityX, velocityY, minX,
                maxX, minY, maxY
            )
            currX = startX
            currY = startY
        }

        fun cancelFling() {
            if (scroller != null) {
                state = State.NONE
                scroller!!.forceFinished(true)
            }
        }

        override fun run() {

            //
            // OnTouchImageViewListener is set: TouchImageView listener has been flung by user.
            // Listener runnable updated with each frame of fling animation.
            //
            touchImageViewListener?.onMove()

            if (scroller!!.isFinished) {
                scroller = null
                return
            }

            if (scroller!!.computeScrollOffset()) {
                val newX = scroller!!.currX
                val newY = scroller!!.currY
                val transX = newX - currX
                val transY = newY - currY
                currX = newX
                currY = newY
                matrix.postTranslate(transX.toFloat(), transY.toFloat())
                fixTrans()
                imageMatrix = matrix
                compatPostOnAnimation(this)
            }
        }
    }


    protected fun getImageWidth(): Float {
        return matchViewWidth * normalizedScale
    }

    protected fun getImageHeight(): Float {
        return matchViewHeight * normalizedScale
    }

}