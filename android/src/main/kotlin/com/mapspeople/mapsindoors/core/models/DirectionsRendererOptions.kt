package com.mapspeople.mapsindoors.core.models

import android.graphics.Color
import com.google.gson.annotations.SerializedName
import com.mapsindoors.core.MPDirectionsRendererConfig
import com.mapsindoors.core.MPRouteAnimationType
import com.mapsindoors.core.MPStrokeStyle

/**
 * The renderer options sent from Dart. Every field is nullable: an absent field means "not set",
 * and must stay out of the config so the solution-served (CMS) style survives the merge.
 *
 * The two enums are typed rather than String so Gson resolves them through the SDK's own
 * @SerializedName values, which is why the Dart enums serialize lowercase.
 */
data class DirectionsRendererOptions(
    @SerializedName("strokeColor") val strokeColor: String? = null,
    @SerializedName("strokeOpacity") val strokeOpacity: Double? = null,
    @SerializedName("strokeWeight") val strokeWeight: Double? = null,
    @SerializedName("strokeStyle") val strokeStyle: MPStrokeStyle? = null,
    @SerializedName("haloEnabled") val haloEnabled: Boolean? = null,
    @SerializedName("haloColor") val haloColor: String? = null,
    @SerializedName("haloOpacity") val haloOpacity: Double? = null,
    @SerializedName("haloWeight") val haloWeight: Double? = null,
    @SerializedName("animationType") val animationType: MPRouteAnimationType? = null,
    @SerializedName("animationSpeed") val animationSpeed: Double? = null,
    @SerializedName("animationMinDuration") val animationMinDuration: Double? = null,
    @SerializedName("animationRepeating") val animationRepeating: Boolean? = null,
    @SerializedName("forceAnimation") val forceAnimation: Boolean? = null,
    @SerializedName("animatedOverlayColor") val animatedOverlayColor: String? = null,
    @SerializedName("animatedOverlayOpacity") val animatedOverlayOpacity: Double? = null,
    @SerializedName("animatedOverlayWeight") val animatedOverlayWeight: Double? = null,
    // fitBounds and fitBoundsPadding are iOS-only. They are accepted and ignored here so that a
    // single Dart options object works unchanged on both platforms.
    @SerializedName("fitBounds") val fitBounds: Boolean? = null,
    @SerializedName("fitBoundsMaxZoom") val fitBoundsMaxZoom: Double? = null,
    @SerializedName("fitBoundsPadding") val fitBoundsPadding: Map<String, Double>? = null,
) {
    /** Throws IllegalArgumentException naming the offending key if a colour cannot be parsed. */
    fun validateColors() {
        mapOf(
            "strokeColor" to strokeColor,
            "haloColor" to haloColor,
            "animatedOverlayColor" to animatedOverlayColor,
        ).forEach { (key, value) ->
            if (value != null) {
                try {
                    Color.parseColor(value)
                } catch (e: IllegalArgumentException) {
                    throw IllegalArgumentException("$key is not a valid color: $value")
                }
            }
        }
    }

    /**
     * Builds the runtime config. A block is created only when at least one of its keys was set,
     * because MPDirectionsRenderer.mergeConfig merges line and halo field by field but replaces
     * every other block wholesale - so an empty-but-present block would erase the CMS style.
     */
    fun toConfig(): MPDirectionsRendererConfig {
        val line = if (strokeColor != null || strokeOpacity != null ||
            strokeWeight != null || strokeStyle != null
        ) {
            MPDirectionsRendererConfig.Line(
                strokeColor = strokeColor,
                strokeOpacity = strokeOpacity,
                strokeWeight = strokeWeight,
                strokeStyle = strokeStyle,
            )
        } else null

        val halo = if (haloEnabled != null || haloColor != null ||
            haloOpacity != null || haloWeight != null
        ) {
            MPDirectionsRendererConfig.Halo(
                enabled = haloEnabled,
                color = haloColor,
                opacity = haloOpacity,
                weight = haloWeight,
            )
        } else null

        val overlay = if (animatedOverlayColor != null || animatedOverlayOpacity != null ||
            animatedOverlayWeight != null
        ) {
            MPDirectionsRendererConfig.Overlay(
                strokeColor = animatedOverlayColor,
                strokeOpacity = animatedOverlayOpacity,
                strokeWeight = animatedOverlayWeight,
            )
        } else null

        val animation = if (animationType != null || animationSpeed != null ||
            animationMinDuration != null || forceAnimation != null || overlay != null
        ) {
            MPDirectionsRendererConfig.Animation(
                type = animationType,
                speed = animationSpeed,
                minAnimationTime = animationMinDuration,
                forceAnimation = forceAnimation,
                overlay = overlay,
            )
        } else null

        val camera = if (fitBoundsMaxZoom != null) {
            MPDirectionsRendererConfig.Camera(fitBoundsMaxZoom = fitBoundsMaxZoom)
        } else null

        return MPDirectionsRendererConfig(
            line = line,
            halo = halo,
            animation = animation,
            camera = camera,
        )
    }
}
