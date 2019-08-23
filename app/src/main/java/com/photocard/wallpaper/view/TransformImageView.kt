package com.photocard.wallpaper.view

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.Display
import android.view.WindowManager
import android.widget.ImageView
import androidx.annotation.IntRange
import androidx.annotation.NonNull
import com.photocard.wallpaper.data.ExifInfo
import com.photocard.wallpaper.util.EglUtils
import com.photocard.wallpaper.util.FastBitmapDrawable
import com.photocard.wallpaper.util.RectUtils

class TransformImageView @JvmOverloads constructor(
    context: Context, attribute: AttributeSet? = null, defStyleAttr: Int = 0
) : ImageView(context, attribute, defStyleAttr) {

    companion object {
        private const val TAG = "TransformImageView"

        private const val RECT_CORNER_POINTS_COORDS: Int = 8
        private const val RECT_CENTER_POINT_COORDS: Int = 2
        private const val MATRIX_VALUES_COUNT: Int = 9
    }

    protected val currentImageCorners = FloatArray(RECT_CORNER_POINTS_COORDS) { 0f }
    protected val currentImageCenter = FloatArray(RECT_CENTER_POINT_COORDS) { 0f }

    protected val matrixValues = FloatArray(MATRIX_VALUES_COUNT) { 0f }

    protected var currentImageMatrix = Matrix()

    protected var thisWidth: Int = -1
    protected var thisHeight: Int = -1

    protected var transformImageListener: TransformImageListener? = null

    private lateinit var initialImageCorners: FloatArray
    private lateinit var initialImageCenter: FloatArray

    protected var bitmapDecoded = false
    protected var bitmapLaidOut = false

    var imageInputPath: String? = null
        private set(value) {
            field = value
        }
    var imageOutputPath: String? = null
        private set(value) {
            field = value
        }
    var exifInfo: ExifInfo? = null
        private set(value) {
            field = value
        }


    var maxBitmapSize: Int = 0
        public get () = if (field <= 0) calculateMaxBitmapSize(context) else field

    interface TransformImageListener {
        fun onLoadComplete()
        fun onLoadFailure(@NonNull e: Exception)
        fun onRotate(currentAngle: Float)
        fun onScale(currentScale: Float)
    }


    override fun setScaleType(scaleType: ScaleType?) {
        if (scaleType == ScaleType.MATRIX) {
            super.setScaleType(scaleType)
        } else {
            Log.w(TAG, "Invalid ScaleType. Only ScaleType.MATRIX can be used")
        }
    }

    fun getCurrentScale() = getMatrixScale(currentImageMatrix)

    fun getMatrixScale(matrix: Matrix) = Math.sqrt(Math.pow(getMatrixValue(matrix, Matrix.MSCALE_X).toDouble(), 2.0))


    fun getCurrentAngle(matrix: Matrix) = getMatrixAngle(currentImageMatrix)

    fun getMatrixAngle(matrix: Matrix) =
        -(Math.atan2(
            getMatrixValue(matrix, Matrix.MSKEW_X).toDouble(),
            getMatrixValue(matrix, Matrix.MSCALE_X).toDouble()
        ) * (180 / Math.PI))

    override fun setImageMatrix(matrix: Matrix?) {
        super.setImageMatrix(matrix)
        currentImageMatrix.set(matrix)
        updateCurrenImagePoints()
    }

    fun getViewBitmap(): Bitmap? =
        if (drawable == null || (drawable is FastBitmapDrawable).not()) null else (drawable as FastBitmapDrawable).bitmap

    fun postTranslate(deltax: Float, deltay: Float) {
        if (deltax != 0f || deltay != 0f) {
            currentImageMatrix.postTranslate(deltax, deltay)
            imageMatrix = currentImageMatrix
        }
    }

    fun postScale(deltaScale: Float, px: Float, py: Float) {
        if (deltaScale != 0f) {
            currentImageMatrix.postScale(deltaScale, deltaScale, px, py)
            imageMatrix = currentImageMatrix
            transformImageListener?.run { onScale(getMatrixScale(currentImageMatrix).toFloat()) }
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (changed || (bitmapDecoded && bitmapLaidOut)) {
            setLeft(paddingLeft)
            setTop(paddingTop)
            setRight(width - paddingRight)
            setBottom(height - paddingBottom)
            thisWidth = right - left
            thisHeight = bottom - top
            onImageLaidOut()
        }
    }

    private fun onImageLaidOut() {
        val drawable: Drawable = drawable ?: return

        val w = drawable.intrinsicWidth.toFloat()
        val h = drawable.intrinsicHeight.toFloat()

        val initImageRectF = RectF(0f, 0f, w, h)

        initialImageCenter = RectUtils.getCornersFromRect(initImageRectF)
        initialImageCorners = RectUtils.getCenterFromRect(initImageRectF)

        bitmapLaidOut = true

        transformImageListener?.run { onLoadComplete() }

    }

    private fun getMatrixValue(
        matrix: Matrix,
        @IntRange(from = 0, to = MATRIX_VALUES_COUNT.toLong()) valueIndex: Int
    ): Float {
        matrix.getValues(matrixValues)
        return matrixValues[valueIndex]
    }

    private fun calculateMaxBitmapSize(context: Context): Int {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display: Display
        val width: Int
        val height: Int
        val size = Point()

        display = wm.defaultDisplay
        display.getSize(size)

        width = size.x
        height = size.y

        // Twice the device screen diagonal as default
        var maxBitmapSize = Math.sqrt(Math.pow(width.toDouble(), 2.0) + Math.pow(height.toDouble(), 2.0)).toInt()

        // Check for max texture size via Canvas
        val canvas = Canvas()
        val maxCanvasSize = Math.min(canvas.maximumBitmapWidth, canvas.maximumBitmapHeight)
        if (maxCanvasSize > 0) {
            maxBitmapSize = Math.min(maxBitmapSize, maxCanvasSize)
        }

        // Check for max texture size via GL
        val maxTextureSize = EglUtils.getMaxTextureSize()
        if (maxTextureSize > 0) {
            maxBitmapSize = Math.min(maxBitmapSize, maxTextureSize)
        }

        Log.d(TAG, "maxBitmapSize: $maxBitmapSize")
        return maxBitmapSize
    }

    private fun updateCurrenImagePoints() {
        currentImageMatrix.mapPoints(currentImageCorners, initialImageCorners)
        currentImageMatrix.mapPoints(initialImageCenter, initialImageCenter)
    }

}