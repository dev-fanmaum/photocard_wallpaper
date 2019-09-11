package com.photocard.wallpaper

import android.content.Context
import android.graphics.*
import android.support.annotation.CallSuper
import android.util.AttributeSet
import android.view.WindowManager
import android.widget.FrameLayout
import kotlin.math.min

open class OverlayViewLayout @JvmOverloads constructor(
    context: Context,
    attributes: AttributeSet? = null,
    defStyleAtt: Int = 0
) : FrameLayout(context, attributes, defStyleAtt) {

    val deviceViewBox: RectF = RectF()

    protected val userDeviceWidth: Float
    protected val userDeviceHeight: Float

    val deviceViewWidth get() = deviceViewBox.right - deviceViewBox.left
    val deviceViewHeight get() = deviceViewBox.bottom - deviceViewBox.top

    private var viewRatioScale = .9f

    private var borderColor = Color.BLACK
    private var borderScaleSize = 0.9f


    init {
        setBackgroundColor(Color.TRANSPARENT)
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

    private fun attributeStting(attributes: AttributeSet?) {
        attributes ?: return

        val typeArray = context.obtainStyledAttributes(attributes, R.styleable.OverlayViewLayout)
        borderScaleSize = typeArray.getDimension(
            R.styleable.OverlayViewLayout_overlay_border_scale_size,
            borderScaleSize
        )
        borderColor =
            typeArray.getColor(R.styleable.OverlayViewLayout_overlay_border_color, borderColor)

    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        deviceSizeSetting(
            MeasureSpec.getSize(widthMeasureSpec),
            MeasureSpec.getSize(heightMeasureSpec)
        )
    }

    private var initMeasureSettingFrag = true
    @CallSuper
    open fun deviceSizeSetting(viewWidth: Int, viewHeight: Int) {
        if (initMeasureSettingFrag) {
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

            initMeasureSettingFrag = false
        }
    }


    override fun onDrawForeground(canvas: Canvas?) {
        super.onDrawForeground(canvas)
        canvas?.drawRect(deviceViewBox, Paint().apply {
            color = Color.BLACK
            strokeWidth = 4f
            style = Paint.Style.STROKE
        })
        canvas?.drawRect(0f, 0f, measuredWidth.toFloat(), measuredHeight.toFloat(), Paint().apply {
            color = Color.BLACK
            strokeWidth = 4f
            style = Paint.Style.STROKE
        })
    }
}