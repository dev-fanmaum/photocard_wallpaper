package com.photocard.wallpaper

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import com.photocard.wallpaper.vo.ZoomVariables
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Created by MyInnos on 28-11-2016.
 */

open class TouchImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : BaseImageView(
    context, attrs, defStyle
) {

    //
    // Matrix applied to image. MSCALE_X and MSCALE_Y should always be equal.
    // MTRANS_X and MTRANS_Y are the other values used. prevMatrix is the nextMatrix
    // saved prior to the screen rotating.
    //

    protected val deviceWidth: Float
    protected val deviceHeight: Float

    protected var deviceForegroundBoxSize: RectF = RectF(0f, 0f, 0f, 0f)

    protected val deviceSizeFromOverlayToWidthSize get() = deviceForegroundBoxSize.right - deviceForegroundBoxSize.left
    protected val deviceSizeFromOverlayToHeightSize get() = deviceForegroundBoxSize.bottom - deviceForegroundBoxSize.top

    private var mScaleType: ScaleType? = null

    private var imageRenderedAtLeastOnce: Boolean = false
    private var onDrawReady: Boolean = false

    private var delayedZoomVariables: ZoomVariables? = null

    private val last = PointF()

    /**
     * Returns false if image is in initial, unzoomed state. False, otherwise.
     *
     * @return true if image is zoomed
     */
    private val isZoomed: Boolean
        get() = normalizedScale != 1f

    /**
     * Return a Rect representing the zoomed image.
     *
     * @return rect representing zoomed image
     */
    val zoomedRect: RectF
        get() {
            if (mScaleType == ScaleType.FIT_XY) {
                throw UnsupportedOperationException("getZoomedRect() not supported with FIT_XY")
            }
            val topLeft = transformCoordinateTouchToBitmap(0f, 0f, true)
            val bottomRight =
                transformCoordinateTouchToBitmap(viewWidth.toFloat(), viewHeight.toFloat(), true)

            val w = drawable.intrinsicWidth.toFloat()
            val h = drawable.intrinsicHeight.toFloat()
            return RectF(topLeft.x / w, topLeft.y / h, bottomRight.x / w, bottomRight.y / h)
        }

    /**
     * Get the max zoom multiplier.
     *
     * @return max zoom multiplier.
     */
    /**
     * Set the max zoom multiplier. Default value: 3.
     *
     * @param max max zoom multiplier.
     */
    var maxZoom: Float
        get() = maxScale
        set(max) {
            maxScale = max
            superMaxScale = SUPER_MAX_MULTIPLIER * maxScale
        }

    /**
     * Get the min zoom multiplier.
     *
     * @return min zoom multiplier.
     */
    /**
     * Set the min zoom multiplier. Default value: 1.
     *
     * @param min min zoom multiplier.
     */
    var minZoom: Float
        get() = minScale
        set(min) {
            minScale = min
            superMinScale = SUPER_MIN_MULTIPLIER * minScale
        }

    /**
     * Get the current zoom. This is the zoom relative to the initial
     * scale, not the original resource.
     *
     * @return current zoom multiplier.
     */
    private val currentZoom: Float
        get() = normalizedScale

    /**
     * Return the point at the center of the zoomed image. The PointF coordinates range
     * in value between 0 and 1 and the focus point is denoted as a fraction from the left
     * and top of the view. For example, the top left corner of the image would be (0, 0).
     * And the bottom right corner would be (1, 1).
     *
     * @return PointF representing the scroll position of the zoomed image.
     */
    private val scrollPosition: PointF?
        get() {
            val drawable = drawable ?: return null
            val drawableWidth = drawable.intrinsicWidth
            val drawableHeight = drawable.intrinsicHeight

            val point = transformCoordinateTouchToBitmap(
                (viewWidth / 2).toFloat(),
                (viewHeight / 2).toFloat(),
                true
            )
            point.x /= drawableWidth.toFloat()
            point.y /= drawableHeight.toFloat()
            return point
        }

    init {
        super.setClickable(true)

        val size = Point()
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
            .getRealSize(size)

        deviceWidth = size.x.toFloat()
        deviceHeight = size.y.toFloat()

        imageMatrix = nextMatrix
        scaleType = ScaleType.MATRIX
        state = (State.NONE)
        onDrawReady = false

        normalizedScale = 1f

        minScale = normalizedScale

        superMinScale = SUPER_MIN_MULTIPLIER * minScale
        superMaxScale = SUPER_MAX_MULTIPLIER * maxScale

        mScaleDetector = ScaleGestureDetector(context, ScaleListener())
        mGestureDetector = GestureDetector(context, GestureListener())
        super.setOnTouchListener(PrivateOnTouchListener())
    }

    override fun setOnTouchListener(l: OnTouchListener) {
        userTouchListener = l
    }

    fun setOnTouchImageViewListener(l: OnTouchImageViewListener) {
        touchImageViewListener = l
    }

    fun setOnDoubleTapListener(l: GestureDetector.OnDoubleTapListener) {
        doubleTapListener = l
    }

    override fun setScaleType(type: ScaleType?) {
        if (type == ScaleType.FIT_START || type == ScaleType.FIT_END) {
            throw UnsupportedOperationException("TouchImageView does not support FIT_START or FIT_END")
        }
        if (type == ScaleType.MATRIX) {
            super.setScaleType(ScaleType.MATRIX)

        } else {
            mScaleType = type
            if (onDrawReady) {
                //
                // If the image is already rendered, scaleType has been called programmatically
                // and the TouchImageView should be updated with the new scaleType.
                //
                setZoom(this)
            }
        }
    }

    override fun getScaleType(): ScaleType? = mScaleType

    /**
     * Save the current nextMatrix and view dimensions
     * in the prevMatrix and prevView variables.
     */
    override fun savePreviousImageValues() {
        if (viewHeight != 0 && viewWidth != 0) {
            nextMatrix.getValues(m)
            prevMatrix.setValues(m)
            prevMatchViewHeight = matchViewHeight
            prevMatchViewWidth = matchViewWidth
            prevViewHeight = viewHeight
            prevViewWidth = viewWidth
        }
    }

    public override fun onSaveInstanceState(): Parcelable? {
        val bundle = Bundle()
        bundle.putParcelable(INSTANCE_STATE, super.onSaveInstanceState())
        bundle.putFloat(SAVE_SCALE, normalizedScale)
        bundle.putFloat(MATCH_VIEW_HEIGHT, matchViewHeight)
        bundle.putFloat(MATCH_VIEW_WIDTH, matchViewWidth)
        bundle.putInt(VIEW_WIDTH, viewWidth)
        bundle.putInt(VIEW_HEIGHT, viewHeight)
        nextMatrix.getValues(m)
        bundle.putFloatArray(NEXT_MATRIX, m)
        bundle.putBoolean(IMAGE_RENDERED, imageRenderedAtLeastOnce)
        return bundle
    }

    public override fun onRestoreInstanceState(state: Parcelable) {
        if (state is Bundle) {
            normalizedScale = state.getFloat(INSTANCE_STATE)
            m = state.getFloatArray(SAVE_SCALE) ?: floatArrayOf()
            prevMatrix.setValues(m)
            prevMatchViewHeight = state.getFloat(MATCH_VIEW_HEIGHT)
            prevMatchViewWidth = state.getFloat(MATCH_VIEW_WIDTH)
            prevViewHeight = state.getInt(VIEW_WIDTH)
            prevViewWidth = state.getInt(VIEW_HEIGHT)
            imageRenderedAtLeastOnce = state.getBoolean(NEXT_MATRIX)
            super.onRestoreInstanceState(state.getParcelable(IMAGE_RENDERED))
            return
        }

        super.onRestoreInstanceState(state)
    }

    override fun onDraw(canvas: Canvas) {
        onDrawReady = true
        imageRenderedAtLeastOnce = true
        if (delayedZoomVariables != null) {
            setZoom(
                delayedZoomVariables!!.scale,
                delayedZoomVariables!!.focusX,
                delayedZoomVariables!!.focusY,
                delayedZoomVariables!!.scaleType
            )
            delayedZoomVariables = null
        }
        super.onDraw(canvas)
    }

    public override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        savePreviousImageValues()
    }

    /**
     * Reset zoom and translation to initial state.
     */
    fun resetZoom() {
        normalizedScale = 1f
        fitImageToView()
    }

    /**
     * Set zoom to the specified scale. Image will be centered around the point
     * (focusX, focusY). These floats range from 0 to 1 and denote the focus point
     * as a fraction from the left and top of the view. For example, the top left
     * corner of the image would be (0, 0). And the bottom right corner would be (1, 1).
     *
     * @param scale
     * @param focusX
     * @param focusY
     * @param scaleType
     */
    @JvmOverloads
    fun setZoom(
        scale: Float,
        focusX: Float = 0.5f,
        focusY: Float = 0.5f,
        scaleType: ScaleType? = mScaleType
    ) {
        //
        // setZoom can be called before the image is on the screen, but at this point,
        // image and view sizes have not yet been calculated in onMeasure. Thus, we should
        // delay calling setZoom until the view has been measured.
        //
        if (!onDrawReady) {
            delayedZoomVariables = ZoomVariables(scale, focusX, focusY, scaleType!!)
            return
        }

        if (scaleType != mScaleType) {
            setScaleType(scaleType)
        }
        resetZoom()
        scaleImage(scale.toDouble(), (viewWidth / 2).toFloat(), (viewHeight / 2).toFloat(), true)
        nextMatrix.getValues(m)
        m[Matrix.MTRANS_X] = -(focusX * imageWidth - viewWidth * 0.5f)
        m[Matrix.MTRANS_Y] = -(focusY * imageHeight - viewHeight * 0.5f)
        nextMatrix.setValues(m)
        fixTrans()
        imageMatrix = nextMatrix
    }

    /**
     * Set zoom parameters equal to another TouchImageView. Including scale, position,
     * and ScaleType.
     *
     * @param
     */
    fun setZoom(img: TouchImageView) {
        val center = img.scrollPosition
        setZoom(img.currentZoom, center!!.x, center.y, img.scaleType)
    }

    /**
     * Set the focus point of the zoomed image. The focus points are denoted as a fraction from the
     * left and top of the view. The focus points can range in value between 0 and 1.
     *
     * @param focusX
     * @param focusY
     */
    fun setScrollPosition(focusX: Float, focusY: Float) {
        setZoom(normalizedScale, focusX, focusY)
    }

    /**
     * Performs boundary checking and fixes the image nextMatrix if it
     * is out of bounds.
     */
    override fun fixTrans() {
        if (initMeasureSettingFlag) return
        nextMatrix.getValues(m)
        val transX = m[Matrix.MTRANS_X]
        val transY = m[Matrix.MTRANS_Y]

        val fixTransX =
            getFixTrans(
                transX,
                deviceForegroundBoxSize.left,
                deviceForegroundBoxSize.right,
                imageWidth
            )
        val fixTransY =
            getFixTrans(
                transY,
                deviceForegroundBoxSize.top,
                deviceForegroundBoxSize.bottom,
                imageHeight
            )
        nextMatrix.postTranslate(fixTransX, fixTransY)

//        if (fixTransX != 0f || fixTransY != 0f) {
//        }
    }

    /**
     * When transitioning from zooming from focus to zoom from center (or vice versa)
     * the image can become unaligned within the view. This is apparent when zooming
     * quickly. When the content size is less than the view size, the content will often
     * be centered incorrectly within the view. fixScaleTrans first calls fixTrans() and
     * then makes sure the image is centered correctly within the view.
     */
    // FIXME : 최소 사이즈 Scale 동작시 정렬되는 기준
    override fun fixScaleTrans() {
        fixTrans()
        nextMatrix.getValues(m)
        if (imageWidth < viewWidth) {
            m[Matrix.MTRANS_X] = (viewWidth - imageWidth) * .5f
        }

        if (imageHeight < viewHeight) {
            m[Matrix.MTRANS_Y] = (viewHeight - imageHeight) * .5f
        }
        nextMatrix.setValues(m)
    }

    private fun getFixTrans(
        trans: Float,
        minSize: Float,
        maxSize: Float,
        contentSize: Float
    ): Float {
        val minTrans = -(minSize + contentSize - maxSize - minSize)
        val maxTrans = (minSize)

        return when {
            trans < minTrans -> -trans + minTrans
            trans > maxTrans -> -trans + maxTrans
            else -> 0f
        }
    }

    private var initMeasureSettingFlag = true

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val drawable = drawable
        if (drawable == null || drawable.intrinsicWidth == 0 || drawable.intrinsicHeight == 0) {
            setMeasuredDimension(0, 0)
            return
        }

        val drawableWidth = drawable.intrinsicWidth
        val drawableHeight = drawable.intrinsicHeight
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        viewWidth = setViewSize(widthMode, widthSize, drawableWidth)
        viewHeight = setViewSize(heightMode, heightSize, drawableHeight)

        // Set view dimensions
        setMeasuredDimension(viewWidth, viewHeight)

        if (initMeasureSettingFlag) {

            if (viewWidth <= 0 || viewHeight <= 0) {
                return
            }
            val viewToDeviceScaleSize =
                min(
                    viewWidth / deviceWidth,
                    viewHeight / deviceHeight
                )
            val widthRatio = deviceWidth * viewToDeviceScaleSize * (1 - OVERLAY_SCALE_SIZE)
            val heightRatio = deviceHeight * viewToDeviceScaleSize * (1 - OVERLAY_SCALE_SIZE)

            val widthInterval = (viewWidth - widthRatio) * .5f
            val heightInterval = (viewHeight - heightRatio) * .5f

            deviceForegroundBoxSize =
                RectF(
                    widthInterval,
                    heightInterval,
                    widthRatio + widthInterval,
                    heightRatio + heightInterval
                )

            initMeasureSettingFlag = false
            fitImageToView()
        }

    }

    /**
     * If the normalizedScale is equal to 1, then the image is made to fit the screen. Otherwise,
     * it is made to fit the screen according to the dimensions of the previous image nextMatrix. This
     * allows the image to maintain its zoom after rotation.
     */
    override fun fitImageToView() {
        val notInitDeviceBoxBoo =
            deviceForegroundBoxSize.run { left <= 0 || right <= 0 || top <= 0 || bottom <= 0 }
        if (notInitDeviceBoxBoo) return

        val drawable = drawable
        if (drawable == null || drawable.intrinsicWidth == 0 || drawable.intrinsicHeight == 0) {
            return
        }

        val drawableWidth = drawable.intrinsicWidth
        val drawableHeight = drawable.intrinsicHeight

        val xValue = deviceSizeFromOverlayToWidthSize / drawableWidth
        val yValue = deviceSizeFromOverlayToHeightSize / drawableHeight
        val valueMaxData = max(xValue, yValue)

        // Center the image
        val redundantXSpace = viewWidth - valueMaxData * drawableWidth
        val redundantYSpace = viewHeight - valueMaxData * drawableHeight
        matchViewWidth = viewWidth - redundantXSpace
        matchViewHeight = viewHeight - redundantYSpace
        if (!isZoomed && !imageRenderedAtLeastOnce) {

            // Stretch and center image to fit view
            nextMatrix.setScale(valueMaxData, valueMaxData)
            nextMatrix.postTranslate(redundantXSpace / 2, redundantYSpace / 2)
        } else {

            // These values should never be 0 or we will set viewWidth and viewHeight
            // to NaN in translateMatrixAfterRotate. To avoid this, call savePreviousImageValues
            // to set them equal to the current values.
            if (prevMatchViewWidth == 0f || prevMatchViewHeight == 0f) {
                savePreviousImageValues()
            }

            prevMatrix.getValues(m)

            // Rescale Matrix after rotation
            m[Matrix.MSCALE_X] = matchViewWidth / drawableWidth * normalizedScale
            m[Matrix.MSCALE_Y] = matchViewHeight / drawableHeight * normalizedScale

            // TransX and TransY from previous nextMatrix
            val transX = m[Matrix.MTRANS_X]
            val transY = m[Matrix.MTRANS_Y]

            // Width
            val prevActualWidth = prevMatchViewWidth * normalizedScale
            val actualWidth = imageWidth
            translateMatrixAfterRotate(
                Matrix.MTRANS_X,
                transX,
                prevActualWidth,
                actualWidth,
                prevViewWidth,
                viewWidth,
                drawableWidth
            )

            // Height
            val prevActualHeight = prevMatchViewHeight * normalizedScale
            val actualHeight = imageHeight
            translateMatrixAfterRotate(
                Matrix.MTRANS_Y,
                transY,
                prevActualHeight,
                actualHeight,
                prevViewHeight,
                viewHeight,
                drawableHeight
            )

            // Set the nextMatrix to the adjusted scale and translate values.
            nextMatrix.setValues(m)
        }
        fixTrans()
        imageMatrix = nextMatrix
    }


    /**
     * AT_MOST -> WarpSize
     * Exactly -> matchSize
     * Unspecified -> 계산해서 처리.
     * */
    private fun setViewSize(mode: Int, size: Int, drawableWidth: Int): Int =
        when (mode) {
            MeasureSpec.EXACTLY -> size

            MeasureSpec.AT_MOST -> max(drawableWidth, size)

            MeasureSpec.UNSPECIFIED -> drawableWidth

            else -> size
        }

    /**
     * After rotating, the nextMatrix needs to be translated. This function finds the area of image
     * which was previously centered and adjusts translations so that is again the center, post-rotation.
     *
     * @param axis          Matrix.MTRANS_X or Matrix.MTRANS_Y
     * @param trans         the value of trans in that axis before the rotation
     * @param prevImageSize the width/height of the image before the rotation
     * @param imageSize     width/height of the image after rotation
     * @param prevViewSize  width/height of view before rotation
     * @param viewSize      width/height of view after rotation
     * @param drawableSize  width/height of drawable
     */
    private fun translateMatrixAfterRotate(
        axis: Int,
        trans: Float,
        prevImageSize: Float,
        imageSize: Float,
        prevViewSize: Int,
        viewSize: Int,
        drawableSize: Int
    ) {
        m[axis] = when {
            imageSize < viewSize -> //
                // The width/height of image is less than the view's width/height. Center it.
                //
                (viewSize - drawableSize * m[Matrix.MSCALE_X]) * 0.5f
            trans > 0 -> //
                // The image is larger than the view, but was not before rotation. Center it.
                //
                -((imageSize - viewSize) * 0.5f)
            else -> {
                //
                // Find the area of the image which was previously centered in the view. Determine its distance
                // from the left/top side of the view as a fraction of the entire image's width/height. Use that percentage
                // to calculate the trans in the new view width/height.
                //
                val percentage = (abs(trans) + 0.5f * prevViewSize) / prevImageSize
                -(percentage * imageSize - viewSize * 0.5f)
            }
        }
    }

    override fun canScrollHorizontally(direction: Int): Boolean {
        nextMatrix.getValues(m)
        val x = m[Matrix.MTRANS_X]
        return if (imageWidth < viewWidth) false
        else if (x >= -1 && direction < 0) false
        else !(abs(x) + viewWidth.toFloat() + 1f >= imageWidth && direction > 0)
    }

    /**
     * Gesture Listener detects a single click or long click and passes that on
     * to the view's listener.
     *
     * @author Ortiz
     */
    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean =
            if (doubleTapListener != null) doubleTapListener!!.onSingleTapConfirmed(e) else performClick()

        override fun onLongPress(e: MotionEvent) {
            performLongClick()
        }

        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            // If a previous fling is still active, it should be cancelled so that two flings
            // are not run simultaneously.
            fling?.cancelFling()
            fling = Fling(velocityX.toInt(), velocityY.toInt())
            compatPostOnAnimation(fling!!)
            return super.onFling(e1, e2, velocityX, velocityY)
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            var consumed = false
            if (doubleTapListener != null) {
                consumed = doubleTapListener!!.onDoubleTap(e)
            }
            if (state === State.NONE) {
                val targetZoom = if (normalizedScale == minScale) maxScale else minScale
                val doubleTap = DoubleTapZoom(targetZoom, e.x, e.y, false)
                compatPostOnAnimation(doubleTap)
                consumed = true
            }
            return consumed
        }

        override fun onDoubleTapEvent(e: MotionEvent): Boolean =
            doubleTapListener?.onDoubleTapEvent(e) ?: false
    }

    /**
     * Responsible for all touch events. Handles the heavy lifting of drag and also sends
     * touch events to Scale Detector and Gesture Detector.
     *
     * @author Ortiz
     */
    private inner class PrivateOnTouchListener : OnTouchListener {

        //
        // Remember last point position for dragging
        //


        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            mScaleDetector?.onTouchEvent(event)
//            mGestureDetector?.onTouchEvent(event)
            val curr = PointF(event.rawX, event.rawY)

            if (state === State.NONE || state === State.DRAG || state === State.FLING) {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> actionDown(curr)

                    MotionEvent.ACTION_MOVE -> actionMove(curr)

                    MotionEvent.ACTION_UP -> actionUp(curr)

                    MotionEvent.ACTION_POINTER_UP -> actionPointerUp(curr)
                }
            }

//            imageMatrix = nextMatrix

            //
            // User-defined OnTouchListener
            //
            userTouchListener?.onTouch(v, event)

            //
            // OnTouchImageViewListener is set: TouchImageView dragged by user.
            //
            touchImageViewListener?.onMove()

            //
            // indicate event was handled
            //
            return true
        }
    }

    private var tempScaleX = 0f
    private var tempScaleY = 0f

    /**
     * ScaleListener detects user two finger scaling and scales image.
     *
     * @author Ortiz
     */
    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
            tempScaleX = scaleX
            tempScaleY = scaleY
            state = (State.ZOOM)
            return true
        }

        override fun onScale(detector: ScaleGestureDetector): Boolean {
//            scaleImage(detector.scaleFactor.toDouble(), detector.focusX, detector.focusY, true)

            Log.i("scaleEventCheck", """
                |scale = $scaleX , $scaleY
                |detector = ${detector.scaleFactor}
                |foucs = ${detector.focusX} , ${detector.focusY}
                |currentSpan = ${detector.currentSpanX} , ${detector.currentSpanY}
                |previousSpan = ${detector.previousSpanX} , ${detector.previousSpanY}
            """.trimIndent())



            animate()
                .scaleX(tempScaleX * detector.scaleFactor)
                .scaleY(tempScaleY * detector.scaleFactor)
                .start()

            //
            // OnTouchImageViewListener is set: TouchImageView pinch zoomed by user.
            //
            if (touchImageViewListener != null) {
                touchImageViewListener!!.onMove()
            }
            return true
        }

        override fun onScaleEnd(detector: ScaleGestureDetector) {
            super.onScaleEnd(detector)
            tempScaleX = scaleX

            tempScaleY = scaleY

            state = (State.NONE)
            var animateToZoomBoundary = false
            var targetZoom = normalizedScale
            if (normalizedScale > maxScale) {
                targetZoom = maxScale
                animateToZoomBoundary = true

            } else if (normalizedScale < minScale) {
                targetZoom = minScale
                animateToZoomBoundary = true
            }

            if (animateToZoomBoundary) {
                val doubleTap = DoubleTapZoom(
                    targetZoom,
                    viewWidth * .5f,
                    viewHeight * .5f,
                    true
                )
                compatPostOnAnimation(doubleTap)
            }
        }
    }

    override fun scaleImage(
        deltaScale: Double,
        focusX: Float,
        focusY: Float,
        stretchImageToSuper: Boolean
    ) {
        var resultDeltaScale = deltaScale

        val lowerScale: Float
        val upperScale: Float
        if (stretchImageToSuper) {
            lowerScale = superMinScale
            upperScale = superMaxScale

        } else {
            lowerScale = minScale
            upperScale = maxScale
        }

        val origScale = normalizedScale
        normalizedScale *= resultDeltaScale.toFloat()
        if (normalizedScale > upperScale) {
            normalizedScale = upperScale
            resultDeltaScale = (upperScale / origScale).toDouble()
        } else if (normalizedScale < lowerScale) {
            normalizedScale = lowerScale
            resultDeltaScale = (lowerScale / origScale).toDouble()
        }

        nextMatrix.postScale(
            resultDeltaScale.toFloat(),
            resultDeltaScale.toFloat(),
            focusX,
            focusY
        )
        fixScaleTrans()
    }

    override fun actionDown(curr: PointF) {
        last.set(x - curr.x, y - curr.y)
        fling?.cancelFling()
        state = (State.DRAG)
    }

    override fun actionMove(curr: PointF) {
        if (state === State.DRAG) {
//            val deltaX = curr.x - last.x
//            val deltaY = curr.y - last.y

            Log.i("PositonData", "x = ${curr.x}, y = ${curr.y}")

            animate()
                .x(curr.x + last.x)
                .y(curr.y + last.y)
                .setDuration(0)
                .start()

//            nextMatrix.postTranslate(deltaX, deltaY)
//            fixTrans()
//            last.set(curr.x, curr.y)
        }
    }

    override fun actionUp(curr: PointF) {
        state = (State.NONE)
        fixTrans()
    }

    override fun actionPointerUp(curr: PointF) {
        state = (State.NONE)
    }

    companion object {

        //
        // SuperMin and SuperMax multipliers. Determine how much the image can be
        // zoomed below or above the zoom boundaries, before animating back to the
        // min/max zoom boundary.
        //
        private const val SUPER_MIN_MULTIPLIER = .75f
        private const val SUPER_MAX_MULTIPLIER = 1.25f

        const val OVERLAY_SCALE_SIZE = .1f

        private const val INSTANCE_STATE = "instanceState"
        private const val SAVE_SCALE = "saveScale"
        private const val MATCH_VIEW_HEIGHT = "matchViewHeight"
        private const val MATCH_VIEW_WIDTH = "matchViewWidth"
        private const val VIEW_WIDTH = "viewWidth"
        private const val VIEW_HEIGHT = "viewHeight"
        private const val NEXT_MATRIX = "nextMatrix"
        private const val IMAGE_RENDERED = "imageRendered"

    }
}