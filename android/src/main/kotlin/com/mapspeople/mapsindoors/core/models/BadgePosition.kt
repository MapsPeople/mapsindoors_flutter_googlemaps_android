package com.mapspeople.mapsindoors.core.models

import com.mapsindoors.core.MPBadgePosition

enum class BadgePosition(val position: String) {
    BOTTOM_RIGHT("BOTTOM_RIGHT"),
    BOTTOM_LEFT("BOTTM_LEFT"),
    TOP_RIGHT("TOP_RIGHT"),
    TOP_LEFT("TOP_LEFT");

    fun toMPBadgePosition() : MPBadgePosition {
        return when (this) {
            BOTTOM_RIGHT -> MPBadgePosition.bottomRight
            BOTTOM_LEFT -> MPBadgePosition.bottomLeft
            TOP_RIGHT -> MPBadgePosition.topRight
            TOP_LEFT -> MPBadgePosition.topLeft
        }
    }

    companion object {
        fun fromMPBadgePosition(mpPosition: MPBadgePosition) : BadgePosition {
            return when (mpPosition) {
                MPBadgePosition.bottomRight -> BOTTOM_RIGHT
                MPBadgePosition.bottomLeft -> BOTTOM_LEFT
                MPBadgePosition.topRight -> TOP_RIGHT
                MPBadgePosition.topLeft -> TOP_LEFT
                else -> BOTTOM_RIGHT
            }
        }
    }
}