package com.photocard.wallpaper.tast

import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import com.photocard.wallpaper.util.WallPaperCallBack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("MissingPermission")
internal fun settingBitmap(
    context: Context,
    bitmap: Bitmap,
    imageSizeWidth: Int,
    imageSizeHeight: Int,
    userDeviceSizeWidth: Int,
    userDeviceSizeHeight: Int,
    cropRect: Rect,
    callback: WallPaperCallBack
) {

    val jobFlow = flowOf(bitmap)
        .map {
            Bitmap.createScaledBitmap(
                it,
                imageSizeWidth,
                imageSizeHeight,
                true
            )
        }.map {
            Bitmap.createBitmap(
                it,
                cropRect.left,
                cropRect.top,
                cropRect.right,
                cropRect.bottom
            )
        }.map {
            Bitmap.createScaledBitmap(
                it,
                userDeviceSizeWidth,
                userDeviceSizeHeight,
                true
            )
        }.catch {
            callback.error(it)
        }

    CoroutineScope(Dispatchers.Default).launch {
        WallpaperManager.getInstance(context)
            .setBitmap(jobFlow.single())
        withContext(Dispatchers.Main){
            callback.complete()
        }
    }
}