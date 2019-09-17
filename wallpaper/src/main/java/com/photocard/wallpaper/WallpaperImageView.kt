package com.photocard.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.net.Uri
import android.util.AttributeSet
import android.widget.ImageView
import kotlin.math.max

internal class WallpaperImageView @JvmOverloads constructor(
    context: Context,
    attributes: AttributeSet? = null,
    defStyleAtt: Int = 0
) : ImageView(context, attributes, defStyleAtt) {

    private var changeEvent: ImageViewSizeChange? = null

    interface ImageViewSizeChange {
        fun onChange(width: Int, height: Int)
    }

    fun setChangeEvent(event: ImageViewSizeChange) {
        this.changeEvent = event
    }

    fun setChangeEvent(event: (Int, Int) -> Unit) {
        this.changeEvent = object : ImageViewSizeChange {
            override fun onChange(width: Int, height: Int) = event(width, height)
        }
    }

    private val overlayBosSize = RectF()

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        val resultRectF: RectF = (parent as? WallPaperView)?.deviceViewBox ?: RectF()
        overlayBosSize.set(resultRectF)
    }

    override fun setImageResource(resId: Int) {
        super.setImageResource(resId)
        fitImageToView()
    }

    override fun setImageBitmap(bm: Bitmap) {
        super.setImageBitmap(bm)
        fitImageToView()
    }

    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        fitImageToView()
    }

    override fun setImageURI(uri: Uri?) {
        super.setImageURI(uri)
        fitImageToView()
    }

    private fun fitImageToView() {
        changeEvent?.onChange(drawable.intrinsicWidth, drawable.intrinsicHeight)
    }


}
