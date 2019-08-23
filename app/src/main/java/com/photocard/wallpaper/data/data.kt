package com.photocard.wallpaper.data

import android.graphics.Bitmap
import android.graphics.RectF
import android.os.Parcel
import android.os.Parcelable

data class ExifInfo(
    var exifOrientation: Int,
    var exifDegrees: Int,
    var exifTranslation: Int
)

data class ImageState(
    var cropRect: RectF,
    var currentImageRect: RectF,
    var currentScale: Float,
    var currentAngle: Float
)

data class CropParameters(
    var maxResultImageSizeX: Int,
    var maxResultImageSizeY: Int,
    var compressFormat: Bitmap.CompressFormat,
    var compressQuality: Int,
    var imageInputPath: String,
    var imageOutputPath: String,
    var exifInfo: ExifInfo
)

class AspectRatio : Parcelable {

    val aspectRatioTitle: String
    val aspectRatioX: Float
    val aspectRatioY: Float

    constructor(aspectRatioTitle: String, aspectRatioX: Float, aspectRatioY: Float) {
        this.aspectRatioTitle = aspectRatioTitle
        this.aspectRatioX = aspectRatioX
        this.aspectRatioY = aspectRatioY
    }

    protected constructor(parcel: Parcel) {
        aspectRatioTitle = parcel.readString()?:""
        aspectRatioX = parcel.readFloat()
        aspectRatioY = parcel.readFloat()
    }

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeString(aspectRatioTitle)
        dest.writeFloat(aspectRatioX)
        dest.writeFloat(aspectRatioY)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<AspectRatio> {
        override fun createFromParcel(parcel: Parcel): AspectRatio {
            return AspectRatio(parcel)
        }

        override fun newArray(size: Int): Array<AspectRatio?> {
            return arrayOfNulls(size)
        }
    }

}