package com.mapspeople.mapsindoors.core.models

import com.google.gson.annotations.SerializedName

import com.mapsindoors.core.MapsIndoors
import com.mapsindoors.core.MPLocation

data class Location(@SerializedName("id") private val id : String) {
    fun toMPLocation() : MPLocation? {
        return MapsIndoors.getLocationById(id)
    }

    companion object {
        fun fromMPLocation(location: MPLocation?) : Location? {
            return if (location != null) {
                Location(location.locationId)
            } else {
                null
            }
        }
    }
}