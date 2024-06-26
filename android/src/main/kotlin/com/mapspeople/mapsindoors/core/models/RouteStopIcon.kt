package com.mapspeople.mapsindoors.core.models

import android.graphics.Color
import android.content.Context
import com.mapsindoors.core.MPRouteStopIconConfig

data class RouteStopIcon(
    val numbered: Boolean,
    val label: String,
    val color: String,
) {
  fun toMPRouteStopIconConfig(context: Context): MPRouteStopIconConfig {
    return MPRouteStopIconConfig.Builder(context).setNumbered(numbered).setLabel(label).setColor(Color.parseColor(color)).build()
  }
}