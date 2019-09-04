package com.photocard.wallpaper

import android.app.Activity
import android.app.WallpaperManager
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.WindowInsets
import androidx.annotation.IntRange
import androidx.annotation.RequiresPermission
import androidx.fragment.app.Fragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.min
import kotlin.reflect.KFunction

class WallPaperSupportImageView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttribute: Int = 0
) : TouchImageView(context, attributeSet, defStyleAttribute) {

    interface WallPaperCallBack {
        fun error(e: Throwable)
        fun complete()
    }

    private var notchInfoValue: List<Rect>? = null

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            notchInfoValue =
                when (context) {
                    is Activity -> (context as Activity).window.decorView.rootWindowInsets?.displayCutout?.boundingRects
                    is Fragment -> (context as Fragment).requireActivity().window.decorView.rootWindowInsets?.displayCutout?.boundingRects
                    else -> null
                }
        }
    }

    private val overlayDrawable: Drawable?

    /**
    100% — FF
    95% — F2
    90% — E6
    85% — D9
    80% — CC
    75% — BF
    70% — B3
    65% — A6
    60% — 99
    55% — 8C
    50% — 80
    45% — 73
    40% — 66
    35% — 59
    30% — 4D
    25% — 40
    20% — 33
    15% — 26
    10% — 1A
    5%  — 0D
    0% —  00
     */
    @IntRange(from = 0, to = 255)
    private val overlayDrawableAlpha: Int

    init {
        if (attributeSet != null) {
            val typeAttr =
                context.obtainStyledAttributes(attributeSet, R.styleable.WallPaperSupportImageView)

            overlayDrawable =
                typeAttr.getDrawable(R.styleable.WallPaperSupportImageView_overlay_drawable)
            overlayDrawable?.invalidateSelf()
            overlayDrawableAlpha =
                typeAttr.getInteger(
                    R.styleable.WallPaperSupportImageView_overlay_drawable_alpha,
                    255
                )
            typeAttr.recycle()
        } else {
            overlayDrawable = null
            overlayDrawableAlpha = 255
        }
    }

    @Volatile
    private var checkWallPaperProcess = false

    private val viewLeftTrans get() = m[Matrix.MTRANS_X].toInt()
    private val viewTopTrans get() = m[Matrix.MTRANS_Y].toInt()

    private val deviceSizeFromOverlayToWidthSize get() = (deviceForegroundBoxSize.right - deviceForegroundBoxSize.left).toInt()
    private val deviceSizeFromOverlayToHeightSize get() = (deviceForegroundBoxSize.bottom - deviceForegroundBoxSize.top).toInt()

    private val viewRatioFromDevice get() = (deviceForegroundBoxSize.bottom - deviceForegroundBoxSize.top) / deviceHeight


    @RequiresPermission(allOf = [android.Manifest.permission.SET_WALLPAPER, android.Manifest.permission.SET_WALLPAPER_HINTS])
    @Synchronized
    fun saveAndCutBitmap(callback: WallPaperCallBack) {
        if (checkWallPaperProcess) return
        checkWallPaperProcess = true
        nextMatrix.getValues(m)

        val transLeft = viewLeftTrans
        val transTop = viewTopTrans

        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )

        val flow = flowOf(bitmap)
            .map(::drawBitmap)
            .map(::resizeBitmap)
            .map { cropSizeBitmap(it, transLeft, transTop) }
            .catch { bitmapToMapReferenceErrorCatch(it, callback) }
            .map(::userDeviceResize)

        CoroutineScope(Dispatchers.Default).launch { flow.collect { setWallPaper(it, callback) } }

    }

    private suspend fun setWallPaper(bitmap: Bitmap, callback: WallPaperCallBack) {
        val wallPaperManager = WallpaperManager.getInstance(context)

        wallPaperManager.setBitmap(bitmap)

        wallPaperManager.suggestDesiredDimensions(
            deviceWidth.toInt(),
            deviceHeight.toInt()
        )

        checkWallPaperProcess = false
        withContext(Dispatchers.Main) { callback.complete() }

    }

    private suspend fun drawBitmap(bitmap: Bitmap): Bitmap {
        val canvas = Canvas(bitmap)
        drawable.draw(canvas)
        return bitmap
    }

    private suspend fun resizeBitmap(bitmap: Bitmap): Bitmap = Bitmap.createScaledBitmap(
        bitmap,
        getImageWidth().toInt(),
        getImageHeight().toInt(),
        true
    )

    private suspend fun cropSizeBitmap(bitmap: Bitmap, left: Int, top: Int): Bitmap {
        val xSize = abs(left - deviceForegroundBoxSize.left.toInt())
        val ySize = abs(top - deviceForegroundBoxSize.top.toInt())

        return Bitmap.createBitmap(
            bitmap,
            xSize,
            ySize,
            min((getImageWidth() - xSize).toInt(), deviceSizeFromOverlayToWidthSize),
            min((getImageHeight() - ySize).toInt(), deviceSizeFromOverlayToHeightSize)
        )
    }

    private suspend fun bitmapToMapReferenceErrorCatch(e: Throwable, callback: WallPaperCallBack) {
        withContext(Dispatchers.Main) { callback.error(e) }
        checkWallPaperProcess = false
        e.printStackTrace()
    }

    private suspend fun userDeviceResize(bitmap: Bitmap): Bitmap =
        Bitmap.createScaledBitmap(bitmap, deviceWidth.toInt(), deviceHeight.toInt(), true)

    override fun onDrawForeground(canvas: Canvas) {
        super.onDrawForeground(canvas)

        if (overlayDrawable == null) {
            val paint = Paint().apply {
                strokeWidth = 4f
                style = Paint.Style.STROKE
            }

            canvas.drawRect(deviceForegroundBoxSize, paint)
        } else {
            overlayDrawable.setBounds(
                0,
                0,
                deviceSizeFromOverlayToWidthSize,
                deviceSizeFromOverlayToHeightSize
            )
            val overlayBitmap = Bitmap.createBitmap(
                deviceSizeFromOverlayToWidthSize,
                deviceSizeFromOverlayToHeightSize,
                Bitmap.Config.ARGB_8888
            )
            overlayDrawable.draw(Canvas(overlayBitmap))


            canvas.drawBitmap(
                overlayBitmap,
                deviceForegroundBoxSize.left,
                deviceForegroundBoxSize.top,
                Paint().apply { alpha = overlayDrawableAlpha }
            )

        }

        val notchMaskPaint = Paint().apply {
            style = Paint.Style.FILL
            color = Color.BLACK
        }

        notchInfoValue?.asSequence()
            ?.map { it.notchInfoReSizeToViewOverlayRatio() }
            ?.map { it to notchMaskPaint }
            ?.forEach { canvas.drawRect(it.first, it.second) }


    }

    private fun Rect.notchInfoReSizeToViewOverlayRatio() = run {
        RectF(
            deviceForegroundBoxSize.left + left * viewRatioFromDevice,
            deviceForegroundBoxSize.top + top * viewRatioFromDevice,
            deviceForegroundBoxSize.left + right * viewRatioFromDevice,
            deviceForegroundBoxSize.top + bottom * viewRatioFromDevice
        )
    }

}

