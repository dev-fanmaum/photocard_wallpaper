package com.photocard.wallpaper

import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import androidx.annotation.CallSuper
import android.util.AttributeSet
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import kotlin.math.min

open class OverlayViewLayout @JvmOverloads constructor(
    context: Context,
    attributes: AttributeSet? = null,
    defStyleAtt: Int = 0
) : FrameLayout(context, attributes, defStyleAtt) {

    private val overlayView by lazy { OverlayView(context) }

    val deviceViewBox: RectF = RectF()

    protected val userDeviceWidth: Float
    protected val userDeviceHeight: Float

    val deviceViewWidth get() = deviceViewBox.right - deviceViewBox.left
    val deviceViewHeight get() = deviceViewBox.bottom - deviceViewBox.top

    private var viewRatioScale = .9f

    private var borderColor = Color.BLACK
    private var outerColor = Color.BLACK
    private var outerAlpha = 150
    private var innerDrawable: Drawable? = null
    private var borderStockWidthSize = resources.displayMetrics.density * 1


    init {
        setBackgroundColor(Color.TRANSPARENT)
        addView(overlayView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        attributeSetting(attributes)
        val size = Point()
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
            .getRealSize(size)

        userDeviceWidth = size.x.toFloat()
        userDeviceHeight = size.y.toFloat()

        deviceViewBox.apply {
            bottom = userDeviceWidth
            right = userDeviceHeight
        }
    }

    override fun addView(child: View?) {
        super.addView(child)
        overlayView.bringToFront()
    }


    private fun attributeSetting(attributes: AttributeSet?) {
        attributes ?: return

        val typeArray = context.obtainStyledAttributes(attributes, R.styleable.OverlayViewLayout)
        borderColor =
            typeArray.getColor(R.styleable.OverlayViewLayout_overlay_border_color, borderColor)
        outerColor =
            typeArray.getColor(R.styleable.OverlayViewLayout_overlay_outer_color, outerColor)

        outerAlpha = typeArray.getInteger(
            R.styleable.OverlayViewLayout_overlay_inner_drawable_alpha,
            outerAlpha
        )
        innerDrawable = typeArray.getDrawable(R.styleable.OverlayViewLayout_overlay_inner_drawable)

        viewRatioScale = typeArray.getFloat(
            R.styleable.OverlayViewLayout_overlay_device_box_scale,
            viewRatioScale
        )

        typeArray.recycle()
    }

    private var onForegroundFlag = true
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        if (onForegroundFlag) {
            deviceSizeSetting(right - left, bottom - top)
        }
    }

    @CallSuper
    open fun deviceSizeSetting(viewWidth: Int, viewHeight: Int) {
        val widthSizeDiv = viewWidth / userDeviceWidth
        val heightSizeDiv = viewHeight / userDeviceHeight

        val deviceOverlayRatio = min(widthSizeDiv, heightSizeDiv) * viewRatioScale

        val overlayWidthSize = (userDeviceWidth * deviceOverlayRatio)
        val overlayHeightSize = (userDeviceHeight * deviceOverlayRatio)

        val widthInterval = (viewWidth - overlayWidthSize) * .5f
        val heightInterval = (viewHeight - overlayHeightSize) * .5f

        deviceViewBox.apply {
            left = widthInterval
            top = heightInterval
            right = widthInterval + overlayWidthSize
            bottom = heightInterval + overlayHeightSize

        }
    }


    inner class OverlayView(context: Context) : View(context) {
        override fun onDraw(canvas: Canvas?) {
            super.onDraw(canvas)
            canvas ?: return
            canvas.drawBitmap(outerBitmap(), 0f, 0f, Paint())
            onForegroundFlag = false
            canvas.drawRect(
                deviceViewBox,
                Paint().apply
                {
                    color = borderColor
                    strokeWidth = borderStockWidthSize
                    style = Paint.Style.STROKE
                })
            canvas.drawRect(0f, 0f, measuredWidth.toFloat(), measuredHeight.toFloat(), Paint().apply
            {
                color = Color.BLACK
                strokeWidth = 4f
                style = Paint.Style.STROKE
            })

            val innerBitmap = innerBitmap()
            if (innerBitmap != null) {
                canvas.drawBitmap(
                    innerBitmap,
                    deviceViewBox.left,
                    deviceViewBox.top,
                    Paint().apply {
                        alpha = outerAlpha
                    }
                )
            }
        }

        private fun outerBitmap(): Bitmap {
            val bitmap = Bitmap.createBitmap(
                measuredWidth, measuredHeight,
                Bitmap.Config.ARGB_8888
            )

            val canvas = Canvas(bitmap)

            canvas.drawRect(
                0f,
                0f,
                measuredWidth.toFloat(),
                measuredHeight.toFloat(),
                Paint().apply {
                    color = outerColor
                })

            canvas.drawRect(deviceViewBox, Paint().apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
            })

            return bitmap
        }

        private fun innerBitmap(): Bitmap? {
            val tempInnerDrawable = innerDrawable
            tempInnerDrawable ?: return null
            tempInnerDrawable.setBounds(
                0, 0,
                deviceViewWidth.toInt(),
                deviceViewHeight.toInt()
            )
            val bitmap = Bitmap.createBitmap(
                deviceViewWidth.toInt(),
                deviceViewHeight.toInt(),
                Bitmap.Config.ARGB_8888
            )

            val canvas = Canvas(bitmap)
            tempInnerDrawable.draw(canvas)

            return bitmap
        }
    }


}