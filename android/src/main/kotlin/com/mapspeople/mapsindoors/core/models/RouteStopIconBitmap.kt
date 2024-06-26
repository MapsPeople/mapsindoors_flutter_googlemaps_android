package com.mapspeople.mapsindoors.core.models

import com.mapsindoors.core.MPRouteStopIconProvider;
import android.graphics.Bitmap

data class RouteStopIconBitmap(val bitmap: Bitmap) : MPRouteStopIconProvider() {
    override fun getImage(): Bitmap {
        return bitmap
    }
}