package com.photocard.wallpaper.vo

import android.widget.ImageView

internal data class ZoomVariables(
    var scale: Float,
    var focusX: Float,
    var focusY: Float,
    var scaleType: ImageView.ScaleType
)
