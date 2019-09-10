package com.photocard.wallpaper

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.WindowManager
import android.widget.FrameLayout
import kotlin.math.min

open class OverlayViewLayout @JvmOverloads constructor(
    context: Context,
    attributes: AttributeSet? = null,
    defStyleAtt: Int = 0
) : FrameLayout(context, attributes, defStyleAtt) {

    private val deviceViewBox: RectF = RectF()
    private var viewRatioScale = .9f

    private var borderColor = Color.BLACK
    private var borderScaleSize = 0.9f


    init {
        setBackgroundColor(Color.TRANSPARENT)
        val size = Point()
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay
            .getRealSize(size)

        val deviceWidth = size.x.toFloat()
        val deviceHeight = size.y.toFloat()

        deviceViewBox.apply {
            bottom = deviceHeight
            right = deviceWidth
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

    private var initMeasureSettingFrag = true

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)

        if (initMeasureSettingFrag) {
            val viewWidth = MeasureSpec.getSize(widthMeasureSpec)
            val viewHeight = MeasureSpec.getSize(heightMeasureSpec)

            val widthSizeDiv = viewWidth / deviceViewBox.right
            val heightSizeDiv = viewHeight / deviceViewBox.bottom

            val deviceOverlayRatio = min(widthSizeDiv, heightSizeDiv) * viewRatioScale

            val overlayWidthSize = deviceViewBox.right * deviceOverlayRatio
            val overlayHeightSize = deviceViewBox.bottom * deviceOverlayRatio

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