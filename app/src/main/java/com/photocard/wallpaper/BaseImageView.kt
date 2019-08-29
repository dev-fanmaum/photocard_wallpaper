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
import android.widget.ImageView
import com.bumptech.glide.RequestBuilder
import com.photocard.wallpaper.util.CompatScroller
import kotlinx.coroutines.*
import java.lang.Runnable

abstract class BaseImageView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ImageView(context, attributeSet, defStyleAttr) {

    //
    // Scale of image ranges from minScale to maxScale, where minScale == 1
    // when the image is stretched to fit view.
    //

    protected var normalizedScale: Float = 0.8f
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

    protected var minScale: Float = .8f
    protected var maxScale: Float = 3f
    protected var superMinScale: Float = 0.toFloat()
    protected var superMaxScale: Float = 0.toFloat()
    protected var m: FloatArray = FloatArray(9)

    protected var fling: Fling? = null

    protected var mScaleDetector: ScaleGestureDetector? = null
    protected var mGestureDetector: GestureDetector? = null
    protected var doubleTapListener: GestureDetector.OnDoubleTapListener? = null
    protected var userTouchListener: OnTouchListener? = null
    protected var touchImageViewListener: OnTouchImageViewLIstener? = null


    protected enum class State { NONE, DRAG, ZOOM, FLING, ANIMATE_ZOOM }

    protected var state: State? = null

    @SuppressLint("ObsoleteSdkInt")
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    protected fun compatPostOnAnimation(runnable: Runnable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) postOnAnimation(runnable)
        else postDelayed(runnable, (1000 / 60).toLong())
    }

    init {
        scaleType = ScaleType.CENTER_CROP

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

    fun setImageGlide(glide: RequestBuilder<Drawable>) {

        val imageLoadAsync = CoroutineScope(Dispatchers.IO).async { glide.submit().get() }

        CoroutineScope(Dispatchers.IO).launch {
            val drawable = imageLoadAsync.await()
            withContext(Dispatchers.Main) { setImageDrawable(drawable) }
        }

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