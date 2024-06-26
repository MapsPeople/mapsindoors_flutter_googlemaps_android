package com.mapspeople.mapsindoors.core.models

import com.google.gson.annotations.SerializedName
import com.mapsindoors.core.MPLocationSettings

/**
 * Used to deserialize MPLocationSettings from dart
 */
data class LocationSettings(
    @SerializedName("selectable") val selectable: Boolean?,
) {
    fun toMPLocationSettings() : MPLocationSettings {
        return MPLocationSettings(selectable)
    }
}