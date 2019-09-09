package com.photocard.wallpaper

import android.app.Activity
import android.app.WallpaperManager
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.util.AttributeSet
import androidx.annotation.IntRange
import androidx.annotation.RequiresPermission
import androidx.core.graphics.ColorUtils
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
import kotlin.reflect.KMutableProperty1

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
        if ( viewWidth <= 0 || viewHeight <= 0) return

        canvas.overlayOuterBackground()

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
            ?.forEach { canvas.cutOutDraw(it.first, it.second, 10f) }


    }

    private fun Rect.notchInfoReSizeToViewOverlayRatio() = run {
        RectF(
            deviceForegroundBoxSize.left + left * viewRatioFromDevice,
            deviceForegroundBoxSize.top + top * viewRatioFromDevice,
            deviceForegroundBoxSize.left + right * viewRatioFromDevice,
            deviceForegroundBoxSize.top + bottom * viewRatioFromDevice
        )
    }

    private fun Canvas.overlayOuterBackground() {

        val tempBitmap = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888)

        val tempCanvas = Canvas(tempBitmap)


        val paint = Paint().apply {
            color = ColorUtils.setAlphaComponent(Color.LTGRAY, 200)
        }
        tempCanvas.drawRect(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat(), paint)
        tempCanvas.drawRect(deviceForegroundBoxSize, Paint().apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        })

        drawBitmap(tempBitmap,0f, 0f, Paint())

    }

    private fun Canvas.cutOutDraw(rect: RectF, paint: Paint, roundSize: Float) {
        val drawPath = Path()
        val roundPositionCheckToLeft = rectCheckPosition(rect, deviceForegroundBoxSize, RectF::left)
        val roundPositionCheckToRight =
            rectCheckPosition(rect, deviceForegroundBoxSize, RectF::right)
        val roundPositionCheckToTop = rectCheckPosition(rect, deviceForegroundBoxSize, RectF::top)
        val roundPositionCheckToBottom =
            rectCheckPosition(rect, deviceForegroundBoxSize, RectF::bottom)

        if (roundPositionCheckToTop || roundPositionCheckToLeft) {
            drawPath.moveTo(rect.left, rect.top)
        } else {
            drawPath.moveTo(rect.left + roundSize, rect.top)
            drawPath.rQuadTo(0f, 0f, -roundSize, roundSize)
        }
        drawPath.lineTo(rect.left, rect.bottom - roundSize)

        if (roundPositionCheckToBottom || roundPositionCheckToLeft)
            drawPath.lineTo(rect.left, rect.bottom)
        else drawPath.rQuadTo(0f, 0f, roundSize, roundSize)
        drawPath.lineTo(rect.right - roundSize, rect.bottom)

        if (roundPositionCheckToBottom || roundPositionCheckToRight)
            drawPath.lineTo(rect.right, rect.bottom)
        else drawPath.rQuadTo(0f, 0f, roundSize, -roundSize)
        drawPath.lineTo(rect.right, rect.top + roundSize)

        if (roundPositionCheckToTop || roundPositionCheckToRight)
            drawPath.lineTo(rect.right, rect.top)
        else drawPath.rQuadTo(0f, 0f, -roundSize, -roundSize)

        drawPath.close()

        this.drawPath(drawPath, paint)
    }


    private fun rectCheckPosition(
        rect: RectF,
        foregroundBox: RectF,
        reflect: KMutableProperty1<RectF, Float>
    ): Boolean = reflect.get(rect) == reflect.get(foregroundBox)

}

