package com.photocard.wallpaper.view

import android.content.Context
import android.graphics.Matrix
import android.util.AttributeSet
import android.widget.ImageView
import androidx.annotation.IntDef
import com.photocard.wallpaper.data.ExifInfo

class TransformImageView @JvmOverloads constructor(
    context: Context, attribute: AttributeSet? = null, defStyleAttr: Int = 0
) : ImageView(context, attribute, defStyleAttr) {

    companion object {
        private const val RECT_CORNER_POINTS_COORDS = 8;
        private const val RECT_CENTER_POINT_COORDS = 2;
        private const val MATRIX_VALUES_COUNT = 9;
    }

    @IntDef(RECT_CORNER_POINTS_COORDS, RECT_CENTER_POINT_COORDS, MATRIX_VALUES_COUNT)
    @Retention(AnnotationRetention.SOURCE)
    annotation class RectOption

    protected val currentImageCorners = Array(RECT_CORNER_POINTS_COORDS) { 0f }
    protected val currentImageCenter = Array(RECT_CENTER_POINT_COORDS) { 0f }

    protected val matrixValues = Array(MATRIX_VALUES_COUNT) { 0f }

    protected var currentImageMatrix = Matrix()

    protected var thisWidth: Int = -1
    protected var thisHeight: Int = -1

    protected var bitmapDecoded = false
    protected var bitmapLaidOut = false


    private var mImageInputPath: String? = null
    private var mImageOutputPath:String? = null
    private var mExifInfo: ExifInfo? = null

}