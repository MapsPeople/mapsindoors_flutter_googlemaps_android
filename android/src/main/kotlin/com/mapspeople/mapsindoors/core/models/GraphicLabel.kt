package com.mapspeople.mapsindoors.core.models

import com.google.gson.annotations.SerializedName
import com.mapsindoors.core.MPLabelGraphic

data class GraphicLabel(
        @SerializedName("backgroundImage") val backgroundImage: String,
        @SerializedName("stretchX") val stretchX: Array<IntArray>,
        @SerializedName("stretchY") val stretchY: Array<IntArray>,
        @SerializedName("content") val content: IntArray,
) {

    fun toMPLabelGraphic() : MPLabelGraphic {
        return MPLabelGraphic(backgroundImage, stretchX, stretchY, content)
    }
}