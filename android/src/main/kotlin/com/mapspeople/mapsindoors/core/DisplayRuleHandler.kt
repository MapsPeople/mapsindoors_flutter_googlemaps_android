package com.mapspeople.mapsindoors.core

import com.google.gson.Gson
import com.mapsindoors.core.*
import com.mapspeople.mapsindoors.core.models.BadgePosition
import com.mapspeople.mapsindoors.core.models.GraphicLabel
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler

class DisplayRuleHandler(messenger: BinaryMessenger, private val getMapView: () -> MapView?) : MethodCallHandler {
    private val displayRuleChannel: MethodChannel
    private val gson = Gson()
    init {
        displayRuleChannel = MethodChannel(messenger, "DisplayRuleMethodChannel")
        displayRuleChannel.setMethodCallHandler(this)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        val id = call.argument<String>("id")!!
        val dr: MPDisplayRule? = when (id) {
            "buildingOutline" -> MapsIndoors.getDisplayRule(MPSolutionDisplayRule.BUILDING_OUTLINE)
            "selectionHighlight" -> MapsIndoors.getDisplayRule(MPSolutionDisplayRule.SELECTION_HIGHLIGHT)
            "positionIndicator" -> MapsIndoors.getDisplayRule(MPSolutionDisplayRule.POSITION_INDICATOR)
            else -> MapsIndoors.getDisplayRule(id)
        }

        fun error(details: Any? = null) {
            result.error("-1", "Argument was null", details)
        }
        fun success(ret : Any? = "success") {
            result.success(ret)
        }

        val method = call.method.drop(4)

        dr?.apply {
            when (method) {
                "isVisible" -> success(isVisible)
                "setVisible" -> {
                    val visible = call.argument<Boolean>("visible")
                    if (visible != null) {
                        isVisible = visible
                        success()
                    } else {
                        error()
                    }
                }
                "getIconSize" -> success(gson.toJson(iconSize))
                "getIconUrl" -> success(iconUrl)
                "getLabel" -> success(label)
                "getLabelMaxWidth" -> success(labelMaxWidth)
                "getLabelZoomFrom" -> success(labelZoomFrom)
                "getLabelZoomTo" -> success(labelZoomTo)
                "getModel2DBearing" -> success(model2DBearing)
                "getModel2DHeightMeters" -> success(model2DHeightMeters)
                "getModel2DModel" -> success(model2DModel)
                "getModel2DZoomTo" -> success(model2DZoomTo)
                "getModel2DWidthMeters" -> success(model2DWidthMeters)
                "getModel2DZoomFrom" -> success(model2DZoomFrom)
                "getPolygonFillColor" -> success(polygonFillColor)
                "getPolygonFillOpacity" -> success(polygonFillOpacity)
                "getPolygonZoomTo" -> success(polygonZoomTo)
                "getPolygonStrokeColor" -> success(polygonStrokeColor)
                "getPolygonStrokeOpacity" -> success(polygonStrokeOpacity)
                "getPolygonStrokeWidth" -> success(polygonStrokeWidth)
                "getPolygonZoomFrom" -> success(polygonZoomFrom)
                "getZoomFrom" -> success(zoomFrom)
                "getZoomTo" -> success(zoomTo)
                "isIconVisible" -> success(isIconVisible)
                "isLabelVisible" -> success(isLabelVisible)
                "isModel2DVisible" -> success(isModel2DVisible)
                "isPolygonVisible" -> success(isPolygonVisible)
                "isValid" -> success(isValid)
                "reset" -> {
                    reset()
                    success()
                }
                "setIcon" -> {
                    val iconUrl = call.argument<String>("url")
                    if (iconUrl != null) {
                        setIcon(iconUrl)
                        success()
                    } else {
                        error()
                    }
                }
                "setIconVisible" -> {
                    val visible = call.argument<Boolean>("visible")
                    if (visible != null) {
                        isIconVisible = visible
                        success()
                    } else {
                        error()
                    }
                }
                "setIconSize" -> {
                    val size = gson.fromJson(call.argument<String>("size"), MPIconSize::class.java)
                    if (size != null) {
                        setIconSize(size.width, size.height)
                        success()
                    } else {
                        error()
                    }
                }
                "setLabel" -> {
                    val label = call.argument<String>("label")
                    if (label != null) {
                        dr.label = label
                        success()
                    } else {
                        error()
                    }
                }
                "setLabelMaxWidth" -> {
                    val maxWidth = call.argument<Number>("maxWidth")
                    if (maxWidth != null) {
                        labelMaxWidth = maxWidth.toInt()
                        success()
                    } else {
                        error()
                    }
                }
                "setLabelVisible" -> {
                    val visible = call.argument<Boolean>("visible")
                    if (visible != null) {
                        isLabelVisible = visible
                        success()
                    } else {
                        error()
                    }
                }
                "setLabelZoomFrom" -> {
                    val zoomFrom = call.argument<Number>("zoomFrom")
                    if (zoomFrom != null) {
                        labelZoomFrom = zoomFrom.toFloat()
                        success()
                    } else {
                        error()
                    }
                }
                "setLabelZoomTo" -> {
                    val zoomTo = call.argument<Number>("zoomTo")
                    if (zoomTo != null) {
                        labelZoomTo = zoomTo.toFloat()
                        success()
                    } else {
                        error()
                    }
                }
                "setModel2DBearing" -> {
                    val bearing = call.argument<Number>("bearing")
                    if (bearing != null) {
                        model2DBearing = bearing.toDouble()
                        success()
                    } else {
                        error()
                    }
                }
                "setModel2DModel" -> {
                    val model = call.argument<String>("model")
                    if (model != null) {
                        model2DModel = model
                        success()
                    } else {
                        error()
                    }
                }
                "setModel2DHeightMeters" -> {
                    val height = call.argument<Number>("height")
                    if (height != null) {
                        model2DHeightMeters = height.toDouble()
                        success()
                    } else {
                        error()
                    }
                }
                "setModel2DVisible" -> {
                    val visible = call.argument<Boolean>("visible")
                    if (visible != null) {
                        isModel2DVisible = visible
                        success()
                    } else {
                        error()
                    }
                }
                "setModel2DWidthMeters" -> {
                    val width = call.argument<Number>("width")
                    if (width != null) {
                        model2DWidthMeters = width.toDouble()
                        success()
                    } else {
                        error()
                    }
                }
                "setModel2DZoomFrom" -> {
                    val zoomFrom = call.argument<Number>("zoomFrom")
                    if (zoomFrom != null) {
                        model2DZoomFrom = zoomFrom.toFloat()
                        success()
                    } else {
                        error()
                    }
                }
                "setModel2DZoomTo" -> {
                    val zoomTo = call.argument<Number>("zoomTo")
                    if (zoomTo != null) {
                        model2DZoomTo =  zoomTo.toFloat()
                        success()
                    } else {
                        error()
                    }
                }
                "setPolygonFillColor" -> {
                    val color = call.argument<String>("color")
                    if (color != null) {
                        polygonFillColor = color
                        success()
                    } else {
                        error()
                    }
                }
                "setPolygonFillOpacity" -> {
                    val opacity = call.argument<Number>("opacity")
                    if (opacity != null) {
                        polygonFillOpacity = opacity.toFloat()
                        success()
                    } else {
                        error()
                    }
                }
                "setPolygonStrokeColor" -> {
                    val color = call.argument<String>("color")
                    if (color != null) {
                        polygonStrokeColor = color
                        success()
                    } else {
                        error()
                    }
                }
                "setPolygonStrokeOpacity" -> {
                    val opacity = call.argument<Number>("opacity")
                    if (opacity != null) {
                        polygonStrokeOpacity = opacity.toFloat()
                        success()
                    } else {
                        error()
                    }
                }
                "setPolygonStrokeWidth" -> {
                    val width = call.argument<Number>("width")
                    if (width != null) {
                        polygonStrokeWidth = width.toFloat()
                        success()
                    } else {
                        error()
                    }
                }
                "setPolygonVisible" -> {
                    val visible = call.argument<Boolean>("visible")
                    if (visible != null) {
                        isPolygonVisible = visible
                        success()
                    } else {
                        error()
                    }
                }
                "setPolygonZoomFrom" -> {
                    val zoomFrom = call.argument<Number>("zoomFrom")
                    if (zoomFrom != null) {
                        polygonZoomFrom = zoomFrom.toFloat()
                        success()
                    } else {
                        error()
                    }
                }
                "setPolygonZoomTo" -> {
                    val zoomTo = call.argument<Number>("zoomTo")
                    if (zoomTo != null) {
                        polygonZoomTo = zoomTo.toFloat()
                        success()
                    } else {
                        error()
                    }
                }
                "getExtrusionColor" -> success(extrusionColor)
                "getExtrusionHeight" -> success(extrusionHeight)
                "getExtrusionZoomFrom" -> success(extrusionZoomFrom)
                "getExtrusionZoomTo" -> success(extrusionZoomTo)
                "getWallColor" -> success(wallColor)
                "getWallHeight" -> success(wallHeight)
                "getWallZoomFrom" -> success(wallZoomFrom)
                "getWallZoomTo" -> success(wallZoomTo)
                "isExtrusionVisible" -> success(isExtrusionVisible)
                "isWallVisible" -> success(isWallVisible)
                "setExtrusionColor" -> {
                    val color = call.argument<String>("color")
                    if (color != null) {
                        extrusionColor = color
                        success()
                    } else {
                        error()
                    }
                }
                "setExtrusionHeight" -> {
                    val height = call.argument<Number>("height")
                    if (height != null) {
                        extrusionHeight = height.toFloat()
                        success()
                    } else {
                        error()
                    }
                }
                "setExtrusionVisible" -> {
                    val visible = call.argument<Boolean>("visible")
                    if (visible != null) {
                        isExtrusionVisible = visible
                        success()
                    } else {
                        error()
                    }
                }
                "setExtrusionZoomFrom" -> {
                    val zoomFrom = call.argument<Number>("zoomFrom")
                    if (zoomFrom != null) {
                        extrusionZoomFrom = zoomFrom.toFloat()
                        success()
                    } else {
                        error()
                    }
                }
                "setExtrusionZoomTo" -> {
                    val zoomTo = call.argument<Number>("zoomTo")
                    if (zoomTo != null) {
                        extrusionZoomTo = zoomTo.toFloat()
                        success()
                    } else {
                        error()
                    }
                }
                "setWallColor" -> {
                    val color = call.argument<String>("color")
                    if (color != null) {
                        wallColor = color
                        success()
                    } else {
                        error()
                    }
                }
                "setWallHeight" -> {
                    val height = call.argument<Number>("height")
                    if (height != null) {
                        wallHeight = height.toFloat()
                        success()
                    } else {
                        error()
                    }
                }
                "setWallVisible" -> {
                    val visible = call.argument<Boolean>("visible")
                    if (visible != null) {
                        isWallVisible = visible
                        success()
                    } else {
                        error()
                    }
                }
                "setWallZoomFrom" -> {
                    val zoomFrom = call.argument<Number>("zoomFrom")
                    if (zoomFrom != null) {
                        wallZoomFrom = zoomFrom.toFloat()
                        success()
                    } else {
                        error()
                    }
                }
                "setWallZoomTo" -> {
                    val zoomTo = call.argument<Number>("zoomTo")
                    if (zoomTo != null) {
                        wallZoomTo = zoomTo.toFloat()
                        success()
                    } else {
                        error()
                    }
                }
                "setZoomFrom" -> {
                    val zoomFromArg = call.argument<Number>("zoomFrom")
                    if (zoomFromArg != null) {
                        zoomFrom = zoomFromArg.toFloat()
                        success()
                    } else {
                        error()
                    }
                }
                "setZoomTo" -> {
                    val zoomToArg = call.argument<Number>("zoomTo")
                    if (zoomToArg != null) {
                        zoomTo = zoomToArg.toFloat()
                        success()
                    } else {
                        error()
                    }
                }
                "isModel3DVisible" -> success(isModel3DVisible)
                "getModel3DZoomFrom" -> success(model3DZoomFrom)
                "getModel3DZoomTo" -> success(model3DZoomTo)
                "getModel3DModel" -> success(model3DModel)
                "getModel3DRotationX" -> success(model3DRotationX)
                "getModel3DRotationY" -> success(model3DRotationY)
                "getModel3DRotationZ" -> success(model3DRotationZ)
                "getModel3DScale" -> success(model3DScale)
                "getBadgeFillColor" -> success(badgeFillColor)
                "getBadgePosition" -> success(BadgePosition.fromMPBadgePosition(badgePosition!!).position)
                "getBadgeRadius" -> success(badgeRadius)
                "getBadgeScale" -> success(badgeScale)
                "getBadgeStrokeColor" -> success(badgeStrokeColor)
                "getBadgeStrokeWidth" -> success(badgeStrokeWidth)
                "getBadgeZoomFrom" -> success(badgeZoomFrom)
                "getBadgeZoomTo" -> success(badgeZoomTo)
                "getExtrusionLightnessFactor" -> success(extrusionLightnessFactor)
                "getIconPlacement" -> success(when (iconPlacement) {
                    MPIconPlacement.ABOVE  -> "ABOVE"
                    MPIconPlacement.BELOW  -> "BELOW"
                    MPIconPlacement.LEFT   -> "LEFT"
                    MPIconPlacement.CENTER -> "CENTER"
                    MPIconPlacement.RIGHT  -> "RIGHT"
                    else -> "ERROR"
                })
                "getLabelStyleBearing" -> success(labelStyleBearing)
                "getLabelStyleHaloBlur" -> success(labelStyleHaloBlur)
                "getLabelStyleHaloColor" -> success(labelStyleHaloColor)
                "getLabelStyleHaloWidth" -> success(labelStyleHaloWidth)
                "getLabelStyleTextColor" -> success(labelStyleTextColor)
                "getLabelStyleTextOpacity" -> success(labelStyleTextOpacity)
                "getLabelStyleTextSize" -> success(labelStyleTextSize)
                "getLabelStylePosition" -> success(
                    when (labelStylePosition) {
                        MPLabelPosition.LEFT   -> 0
                        MPLabelPosition.TOP    -> 1
                        MPLabelPosition.BOTTOM -> 2
                        MPLabelPosition.RIGHT  -> 3
                        else -> null
                    }
                )
                "getLabelType" -> success(labelType?.typeValue)
                "getPolygonLightnessFactor" -> success(polygonLightnessFactor)
                "getWallLightnessFactor" -> success(wallLightnessFactor)
                "isBadgeVisible" -> success(isBadgeVisible)
                "setModel3DVisible" -> {
                    val visibleArg = call.argument<Boolean>("visible")
                    if (visibleArg != null) {
                        isModel3DVisible = visibleArg
                        success()
                    } else {
                        error()
                    }
                }
                "setBadgeFillColor" -> {
                    val color = call.argument<String>("color")
                    if (color != null) {
                        badgeFillColor = color
                        success()
                    } else {
                        error()
                    }
                }
                "setModel3DZoomFrom" -> {
                    val zoomFromArg = call.argument<Double>("zoomFrom")
                    if (zoomFromArg != null) {
                        model3DZoomFrom = zoomFromArg.toFloat()
                        success()
                    } else {
                        error()
                    }
                }
                "setBadgePosition" -> {
                    val position = gson.fromJson(call.argument<String>("position"), BadgePosition::class.java)
                    if (position != null) {
                        badgePosition = position.toMPBadgePosition()
                        success()
                    } else {
                        error()
                    }
                }
                "setModel3DZoomTo" -> {
                    val zoomToArg = call.argument<Double>("zoomTo")
                    if (zoomToArg != null) {
                        model3DZoomTo = zoomToArg.toFloat()
                        success()
                    } else {
                        error()
                    }
                }
                "setBadgeRadius" -> {
                    val radius = call.argument<Number>("radius")
                    if (radius != null) {
                        badgeRadius = radius.toInt()
                        success()
                    } else {
                        error()
                    }
                }
                "setModel3DModel" -> {
                    val modelArg = call.argument<String>("model")
                    if (modelArg != null) {
                        setModel3DModel(modelArg)
                        success()
                    } else {
                        error()
                    }
                }
                "setBadgeScale" -> {
                    val scale = call.argument<Number>("scale")
                    if (scale != null) {
                        badgeScale = scale.toFloat()
                        success()
                    } else {
                        error()
                    }
                }
                "setModel3DRotationX" -> {
                    val rotationArg = call.argument<Double>("rotation")
                    if (rotationArg != null) {
                        model3DRotationX = rotationArg.toFloat()
                        success()
                    } else {
                        error()
                    }
                }
                "setBadgeStrokeColor" -> {
                    val color = call.argument<String>("color")
                    if (color != null) {
                        badgeStrokeColor = color
                        success()
                    } else {
                        error()
                    }
                }
                "setModel3DRotationY" -> {
                    val rotationArg = call.argument<Double>("rotation")
                    if (rotationArg != null) {
                        model3DRotationY = rotationArg.toFloat()
                        success()
                    } else {
                        error()
                    }
                }
                "setBadgeStrokeWidth" -> {
                    val width = call.argument<Number>("width")
                    if (width != null) {
                        badgeStrokeWidth = width.toFloat()
                        success()
                    } else {
                        error()
                    }
                }
                "setModel3DRotationZ" -> {
                    val rotationArg = call.argument<Double>("rotation")
                    if (rotationArg != null) {
                        model3DRotationZ = rotationArg.toFloat()
                        success()
                    } else {
                        error()
                    }
                }
                "setBadgeVisible" -> {
                    val visible = call.argument<Boolean>("visible")
                    if (visible != null) {
                        isBadgeVisible = visible
                        success()
                    } else {
                        error()
                    }
                }
                "setModel3DScale" -> {
                    val scaleArg = call.argument<Double>("scale")
                    if (scaleArg != null) {
                        model3DScale = scaleArg.toFloat()
                        success()
                    } else {
                        error()
                    }
                }
                "setBadgeZoomFrom" -> {
                    val zoomFrom = call.argument<Number>("zoomFrom")
                    if (zoomFrom != null) {
                        badgeZoomFrom = zoomFrom.toFloat()
                        success()
                    } else {
                        error()
                    }
                }
                "setBadgeZoomTo" -> {
                    val zoomTo = call.argument<Number>("zoomTo")
                    if (zoomTo != null) {
                        badgeZoomTo = zoomTo.toFloat()
                        success()
                    } else {
                        error()
                    }
                }
                "setExtrusionLightnessFactor" -> {
                    val factor = call.argument<Number>("factor")
                    if (factor != null) {
                        extrusionLightnessFactor = factor.toFloat()
                        success()
                    } else {
                        error()
                    }
                }
                "setIconPlacement" -> {
                    val placement = gson.fromJson(call.argument<String>("placement"), MPIconPlacement::class.java)
                    if (placement != null) {
                        iconPlacement = placement
                        success()
                    } else {
                        error()
                    }
                }
                "setLabelStyleBearing" -> {
                    val bearing = call.argument<Number>("bearing")
                    if (bearing != null) {
                        labelStyleBearing = bearing.toFloat()
                        success()
                    } else {
                        error()
                    }
                }
                "setLabelStyleHaloBlur" -> {
                    val blur = call.argument<Number>("blur")
                    if (blur != null) {
                        labelStyleHaloBlur = blur.toInt()
                        success()
                    } else {
                        error()
                    }
                }
                "setLabelStyleHaloColor" -> {
                    val color = call.argument<String>("color")
                    if (color != null) {
                        labelStyleHaloColor = color
                        success()
                    } else {
                        error()
                    }
                }
                "setLabelStyleHaloWidth" -> {
                    val width = call.argument<Number>("width")
                    if (width != null) {
                        labelStyleHaloWidth = width.toInt()
                        success()
                    } else {
                        error()
                    }
                }
                "setLabelStyleTextColor" -> {
                    val color = call.argument<String>("color")
                    if (color != null) {
                        labelStyleTextColor = color
                        success()
                    } else {
                        error()
                    }
                }
                "setLabelStyleTextOpacity" -> {
                    val opacity = call.argument<Number>("opacity")
                    if (opacity != null) {
                        labelStyleTextOpacity = opacity.toFloat()
                        success()
                    } else {
                        error()
                    }
                }
                "setLabelStyleTextSize" -> {
                    val size = call.argument<Number>("size")
                    if (size != null) {
                        labelStyleTextSize = size.toInt()
                        success()
                    } else {
                        error()
                    }
                }
                "setLabelStylePosition" -> {
                    val position = when (call.argument<Number>("position")) {
                        0 -> MPLabelPosition.LEFT
                        1 -> MPLabelPosition.TOP
                        2 -> MPLabelPosition.BOTTOM
                        3 -> MPLabelPosition.RIGHT
                        else -> null
                    }

                    if (position != null) {
                        labelStylePosition = position
                        success()
                    } else {
                        error()
                    }
                }
                "setLabelType" -> {
                    val type = gson.fromJson(call.argument<String>("type"), MPLabelType::class.java)
                    if (type != null) {
                        labelType = type
                        success()
                    } else {
                        error()
                    }
                }
                "setPolygonLightnessFactor" -> {
                    val factor = call.argument<Number>("factor")
                    if (factor != null) {
                        polygonLightnessFactor = factor.toFloat()
                        success()
                    } else {
                        error()
                    }
                }
                "setWallLightnessFactor" -> {
                    val factor = call.argument<Number>("factor")
                    if (factor != null) {
                        wallLightnessFactor = factor.toFloat()
                        success()
                    } else {
                        error()
                    }
                }
                "getLabelStyleGraphic" -> success(gson.toJson(labelStyleGraphic))
                "setLabelStyleGraphic" -> {
                    val graphic = gson.fromJson(call.argument<String>("graphic"), GraphicLabel::class.java)?.toMPLabelGraphic()
                    if (graphic != null) {
                        labelStyleGraphic = graphic
                        success()
                    } else {
                        error()
                    }
                }
                "getLabelStylePosition" -> success(labelStylePosition?.ordinal)
                "setLabelStylePosition" -> {
                    val position = call.argument<Number>("position")
                    if (position != null) {
                        labelStylePosition = MPLabelPosition.values()[position.toInt()]
                        success()
                    } else {
                        error()
                    }
                }
                else -> {
                    result.notImplemented()
                }
            }
            getMapView()?.mapControl?.refresh()
            return@onMethodCall
        }
        success(null)
    }
}