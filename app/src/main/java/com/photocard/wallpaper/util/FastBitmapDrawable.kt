/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.photocard.wallpaper.util

import android.graphics.*
import android.graphics.drawable.Drawable

class FastBitmapDrawable(b: Bitmap) : Drawable() {

    private val mPaint = Paint(Paint.FILTER_BITMAP_FLAG)

    var bitmap: Bitmap? = null
        set(b) {
            field = b
            if (b != null) {
                mWidth = bitmap!!.width
                mHeight = bitmap!!.height
            } else {
                mHeight = 0
                mWidth = 0
            }
        }
    private var mAlpha: Int = 0
    private var mWidth: Int = 0
    private var mHeight: Int = 0

    init {
        mAlpha = 255
        bitmap = b
    }

    override fun draw(canvas: Canvas) {
        if (bitmap != null && !bitmap!!.isRecycled) {
            canvas.drawBitmap(bitmap!!, null, bounds, mPaint)
        }
    }

    override fun setColorFilter(cf: ColorFilter?) {
        mPaint.colorFilter = cf
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    override fun setFilterBitmap(filterBitmap: Boolean) { mPaint.isFilterBitmap = filterBitmap
    }

    override fun getAlpha(): Int = mAlpha

    override fun setAlpha(alpha: Int) {
        mAlpha = alpha
        mPaint.alpha = alpha
    }

    override fun getIntrinsicWidth(): Int = mWidth

    override fun getIntrinsicHeight(): Int = mHeight

    override fun getMinimumWidth(): Int = mWidth

    override fun getMinimumHeight(): Int = mHeight

}
