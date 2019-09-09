package com.photocard.wallpaper

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.ScaleGestureDetector
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import com.photocard.wallpaper.util.CompatScroller
import kotlin.math.max
import kotlin.math.min

abstract class BaseImageView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ImageView(context, attributeSet, defStyleAttr) {

    //
    // Scale of image ranges from minScale to maxScale, where minScale == 1
    // when the image is stretched to fit view.
    //

    protected var normalizedScale: Float = 1f
    //
    // Size of view and previous view size (ie before rotation)
    //
    protected var viewWidth: Int = 0
    protected var viewHeight: Int = 0
    protected var prevViewWidth: Int = 0
    protected var prevViewHeight: Int = 0

    //
    // Size of image when it is stretched to fit view. Before and After rotation.
    //
    protected var matchViewWidth: Float = 0.toFloat()
    protected var matchViewHeight: Float = 0.toFloat()
    protected var prevMatchViewWidth: Float = 0.toFloat()
    protected var prevMatchViewHeight: Float = 0.toFloat()

    protected var minScale: Float = 1f
    protected var maxScale: Float = 3f
    protected var superMinScale: Float = 0.toFloat()
    protected var superMaxScale: Float = 0.toFloat()


    protected val nextMatrix: Matrix = Matrix()
    protected val prevMatrix: Matrix = Matrix()

    @Volatile
    protected var m: FloatArray = FloatArray(9)

    protected var fling: Fling? = null

    protected var mScaleDetector: ScaleGestureDetector? = null
    protected var mGestureDetector: GestureDetector? = null
    protected var doubleTapListener: GestureDetector.OnDoubleTapListener? = null
    protected var userTouchListener: OnTouchListener? = null
    protected var touchImageViewListener: OnTouchImageViewListener? = null


    protected enum class State { NONE, DRAG, ZOOM, FLING, ANIMATE_ZOOM }

    protected var state: State? = null

    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    protected fun compatPostOnAnimation(runnable: Runnable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) postOnAnimation(runnable)
        else postDelayed(runnable, (1000 / 60).toLong())
    }

    override fun setImageResource(resId: Int) {
        super.setImageResource(resId)
        savePreviousImageValues()
        fitImageToView()
    }

    override fun setImageBitmap(bm: Bitmap) {
        super.setImageBitmap(bm)
        savePreviousImageValues()
        fitImageToView()
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        savePreviousImageValues()
        fitImageToView()
    }

    override fun setImageURI(uri: Uri?) {
        super.setImageURI(uri)
        savePreviousImageValues()
        fitImageToView()
    }

    protected abstract fun fixTrans()

    protected abstract fun actionDown(curr: PointF)
    protected abstract fun actionMove(curr: PointF)
    protected abstract fun actionUp(curr: PointF)
    protected abstract fun actionPointerUp(curr: PointF)

    protected abstract fun savePreviousImageValues()
    protected abstract fun fitImageToView()


    /**
     * Fling launches sequential runnables which apply
     * the fling graphic to the image. The values for the translation
     * are interpolated by the Scroller.
     *
     * @author Ortiz
     */
    protected inner class Fling internal constructor(velocityX: Int, velocityY: Int) : Runnable {

        private var scroller: CompatScroller? = null
        private var currX: Int = 0
        private var currY: Int = 0

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
                startX, startY,
                velocityX, velocityY,
                minX, maxX,
                minY, maxY
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

    /**
     * DoubleTapZoom calls a series of runnables which apply
     * an animated zoom in/out graphic to the image.
     *
     * @author Ortiz
     */
    protected inner class DoubleTapZoom internal constructor(
        private val targetZoom: Float,
        focusX: Float,
        focusY: Float,
        private val stretchImageToSuper: Boolean
    ) : Runnable {

        private val startTime: Long
        private val startZoom: Float
        private val bitmapX: Float
        private val bitmapY: Float
        private val interpolator = AccelerateDecelerateInterpolator()
        private val startTouch: PointF
        private val endTouch: PointF

        init {
            state = (State.ANIMATE_ZOOM)
            startTime = System.currentTimeMillis()
            this.startZoom = normalizedScale
            val bitmapPoint = transformCoordinateTouchToBitmap(focusX, focusY, false)
            this.bitmapX = bitmapPoint.x
            this.bitmapY = bitmapPoint.y

            //
            // Used for translating image during scaling
            //
            startTouch = transformCoordinateBitmapToTouch(bitmapX, bitmapY)
            endTouch = PointF((viewWidth / 2).toFloat(), (viewHeight / 2).toFloat())
        }

        override fun run() {
            val t = interpolate()
            val deltaScale = calculateDeltaScale(t)
            scaleImage(deltaScale, bitmapX, bitmapY, stretchImageToSuper)
            translateImageToCenterTouchPosition(t)
            fixScaleTrans()
            imageMatrix = nextMatrix

            //
            // OnTouchImageViewListener is set: double tap runnable updates listener
            // with every frame.
            //
            touchImageViewListener?.onMove()

            if (t < 1f) {
                //
                // We haven't finished zooming
                //
                compatPostOnAnimation(this)

            } else {
                //
                // Finished zooming
                //
                state = (State.NONE)
            }
        }

        /**
         * Interpolate between where the image should start and end in order to translate
         * the image so that the point that is touched is what ends up centered at the end
         * of the zoom.
         *
         * @param t
         */
        private fun translateImageToCenterTouchPosition(t: Float) {
            val targetX = startTouch.x + t * (endTouch.x - startTouch.x)
            val targetY = startTouch.y + t * (endTouch.y - startTouch.y)
            val curr = transformCoordinateBitmapToTouch(bitmapX, bitmapY)
            nextMatrix.postTranslate(targetX - curr.x, targetY - curr.y)
        }

        /**
         * Use interpolator to get t
         *
         * @return
         */
        private fun interpolate(): Float {
            val currTime = System.currentTimeMillis()
            var elapsed = (currTime - startTime) / zoomTime
            elapsed = min(1f, elapsed)
            return interpolator.getInterpolation(elapsed)
        }

        /**
         * Interpolate the current targeted zoom and get the delta
         * from the current zoom.
         *
         * @param t
         * @return
         */
        private fun calculateDeltaScale(t: Float): Double {
            val zoom = (startZoom + t * (targetZoom - startZoom)).toDouble()
            return zoom / normalizedScale
        }

        private val zoomTime = 500f
    }

    /**
     * This function will transform the coordinates in the touch event to the coordinate
     * system of the drawable that the imageview contain
     *
     * @param x            x-coordinate of touch event
     * @param y            y-coordinate of touch event
     * @param clipToBitmap Touch event may occur within view, but outside image content. True, to clip return value
     * to the bounds of the bitmap size.
     * @return Coordinates of the point touched, in the coordinate system of the original drawable.
     */
    protected fun transformCoordinateTouchToBitmap(
        x: Float,
        y: Float,
        clipToBitmap: Boolean
    ): PointF {
        nextMatrix.getValues(m)
        val origW = drawable.intrinsicWidth.toFloat()
        val origH = drawable.intrinsicHeight.toFloat()
        val transX = m[Matrix.MTRANS_X]
        val transY = m[Matrix.MTRANS_Y]
        var finalX = (x - transX) * origW / getImageWidth()
        var finalY = (y - transY) * origH / getImageHeight()

        if (clipToBitmap) {
            finalX = min(max(finalX, 0f), origW)
            finalY = min(max(finalY, 0f), origH)
        }

        return PointF(finalX, finalY)
    }

    /**
     * Inverse of transformCoordinateTouchToBitmap. This function will transform the coordinates in the
     * drawable's coordinate system to the view's coordinate system.
     *
     * @param bx x-coordinate in original bitmap coordinate system
     * @param by y-coordinate in original bitmap coordinate system
     * @return Coordinates of the point in the view's coordinate system.
     */
    private fun transformCoordinateBitmapToTouch(bx: Float, by: Float): PointF {
        nextMatrix.getValues(m)
        val origW = drawable.intrinsicWidth.toFloat()
        val origH = drawable.intrinsicHeight.toFloat()
        val px = bx / origW
        val py = by / origH
        val finalX = m[Matrix.MTRANS_X] + getImageWidth() * px
        val finalY = m[Matrix.MTRANS_Y] + getImageHeight() * py
        return PointF(finalX, finalY)
    }

    protected fun getImageWidth(): Float {
        return matchViewWidth * normalizedScale
    }

    protected fun getImageHeight(): Float {
        return matchViewHeight * normalizedScale
    }

    protected abstract fun fixScaleTrans()

    protected abstract fun scaleImage(
        deltaScale: Double,
        focusX: Float,
        focusY: Float,
        stretchImageToSuper: Boolean
    )

}