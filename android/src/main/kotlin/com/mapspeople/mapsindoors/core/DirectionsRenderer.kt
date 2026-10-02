package com.mapspeople.mapsindoors.core

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.mapsindoors.core.MPCameraViewFitMode
import com.mapsindoors.core.MPDirectionsRenderer
import com.mapsindoors.core.MPDirectionsRendererOptions
import com.mapsindoors.core.MPRoute
import com.mapsindoors.core.MapControl
import com.mapsindoors.core.MPRouteStopIconConfig
import com.mapsindoors.core.MPRouteStopIconProvider
import com.mapspeople.mapsindoors.core.models.DirectionsRendererOptions
import com.mapspeople.mapsindoors.core.models.RouteStopIcon
import com.mapspeople.mapsindoors.core.models.RouteStopIconBitmap
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.graphics.Bitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.URLDecoder

class DirectionsRenderer(private val context: Context, binaryMessenger: BinaryMessenger) : MethodCallHandler {
    private val directionsRendererChannel = MethodChannel(binaryMessenger, "DirectionsRendererMethodChannel")
    private val gson = Gson()
    private var mpDirectionsRenderer: MPDirectionsRenderer? = null
    private var mMapControl: MapControl? = null

    // The runtime override, kept here so it can be applied once the renderer exists and read back by
    // getOptions. The SDK cannot answer getOptions for us: we write through setConfig, which leaves
    // MPDirectionsRenderer.getOptions() null, while getConfig() would report the CMS-merged effective
    // style rather than the override that was set.
    //
    // Deliberately an instance field, not a companion one. setMapControl builds a brand new
    // MPDirectionsRenderer, which starts with no runtime config, so a cache outliving this handler
    // would re-apply an override the SDK no longer holds - and, because nothing detaches on a Flutter
    // hot restart, would carry it into the next Dart session.
    private var currentOptions: DirectionsRendererOptions? = null
    private var currentOptionsJson: String? = null

    init {
        directionsRendererChannel.setMethodCallHandler(this)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        val method = call.method.drop(4)
        when (method) {
            "clear" -> {
                mpDirectionsRenderer?.clear()
                result.success("success")
            }
            "finishGuidance" -> {
                mpDirectionsRenderer?.finishGuidance(call.argument<Double?>("usagePercentage"))
                result.success("success")
            }
            "getSelectedLegFloorIndex" -> {
                var selectedLegFloorIndex = mpDirectionsRenderer?.getSelectedLegFloorIndex()
                result.success(selectedLegFloorIndex)
            }
            "nextLeg" -> {
                mpDirectionsRenderer?.nextLeg()
                result.success("success")
            }
            "previousLeg" -> {
                mpDirectionsRenderer?.previousLeg()
                result.success("success")
            }
            "selectLegIndex" -> {
                val int = call.argument<Int?>("legIndex")
                if (int != null) {
                    try {
                        mpDirectionsRenderer?.selectLegIndex(int)
                    } catch (e: java.lang.IllegalStateException) {
                        result.error("-1", e.message, call.method)
                    }
                }
                result.success("success")
            }
            "setAnimatedPolyline" -> {
                val animated = call.argument<Boolean?>("animated")
                val repeated = call.argument<Boolean?>("repeating")
                val durationMs = call.argument<Int?>("durationMs")
                if (animated != null && repeated != null && durationMs != null) {
                    mpDirectionsRenderer?.setAnimatedPolyline(animated, repeated, durationMs)
                }
                result.success("success")
            }
            "setOptions" -> {
                val json = call.argument<String>("options")
                if (json == null) {
                    result.error("-1", "options argument is missing", call.method)
                    return
                }
                val options = try {
                    gson.fromJson(json, DirectionsRendererOptions::class.java)
                        ?: DirectionsRendererOptions()
                } catch (e: Exception) {
                    result.error("-1", e.message, call.method)
                    return
                }
                try {
                    options.validateColors()
                } catch (e: IllegalArgumentException) {
                    result.error("-1", e.message, call.method)
                    return
                }
                currentOptions = options
                currentOptionsJson = json
                applyOptions(options)
                result.success("success")
            }
            "getOptions" -> {
                result.success(currentOptionsJson)
            }
            "clearOptions" -> {
                currentOptions = null
                currentOptionsJson = null
                // Null restores the solution-served config. animationRepeating rides on
                // MPDirectionsRendererOptions instead and has no config slot, so it is not reset.
                // Deliberate rather than overlooked: restoring it means another setOptions call,
                // which re-applies the legacy colour, weight and animation-timing side effects
                // described in applyOptions. Leaving one boolean where the caller put it is the
                // smaller surprise.
                mpDirectionsRenderer?.setConfig(null)
                result.success("success")
            }
            "setCameraAnimationDuration" -> {
                val durationMs = call.argument<Int?>("durationMs")
                if (durationMs != null) {
                    mpDirectionsRenderer?.setCameraAnimationDuration(durationMs)
                }
                result.success("success")
            }
            "setCameraViewFitMode" -> {
                val cameraFitMode = call.argument<Int?>("cameraFitMode")
                var cameraFitModeEnum: MPCameraViewFitMode? = null
                when (cameraFitMode) {
                    0 -> cameraFitModeEnum = MPCameraViewFitMode.NORTH_ALIGNED
                    1 -> cameraFitModeEnum = MPCameraViewFitMode.FIRST_STEP_ALIGNED
                    2 -> cameraFitModeEnum = MPCameraViewFitMode.START_TO_END_ALIGNED
                    3 -> cameraFitModeEnum = MPCameraViewFitMode.NONE
                    else -> {
                        result.error("-1", "$cameraFitMode is not supported", call.method)
                        return
                    }
                }
                mpDirectionsRenderer?.setCameraViewFitMode(cameraFitModeEnum)
                result.success("success")
            }
            "setOnLegSelectedListener" -> {
                mpDirectionsRenderer?.setOnLegSelectedListener {
                    directionsRendererChannel.invokeMethod("onLegSelected", it)
                }
                result.success("success")
            }
            "setPolyLineColors" -> {
                val foreground: Int
                val background: Int
                try {
                    foreground = Color.parseColor(call.argument<String>("foreground"))
                    background = Color.parseColor(call.argument<String>("background"))
                } catch(e: java.lang.IllegalArgumentException) {
                    result.error("-1", "${e.message}: ${call.argument<String>("foreground")}, ${call.argument<String>("background")}", call.method)
                    return
                }
                mpDirectionsRenderer?.setPolylineColors(foreground, background)
                result.success("success")
            }
            "setRoute" -> {
                val route = try {
                    gson.fromJson(call.argument<String>("route"), MPRoute::class.java)
                } catch (e: Exception) {
                    result.error("-1", e.message, call.method)
                    return
                }
                val stopIconString: Map<Int, String>? = call.argument<Map<Int, String>>("stopIcons")
                // no icons set, run "normally"
                if (stopIconString == null) {
                    mpDirectionsRenderer?.setRoute(route)
                    result.success("success")
                    return
                }
                val stopIcons: HashMap<Int, MPRouteStopIconProvider> = hashMapOf()
                CoroutineScope(Dispatchers.Default).launch { 
                    for ((key, value) in stopIconString!!) {
                        val uri = Uri.parse(value)
                        if (uri?.scheme == "mapsindoors") {
                            stopIcons[key] = gson.fromJson(uri.lastPathSegment, RouteStopIcon::class.java)?.toMPRouteStopIconConfig(context) ?: continue
                        } else if (uri?.scheme == "http" || uri?.scheme == "https") {
                            val futureTarget: FutureTarget<Bitmap> = Glide.with(context).asBitmap().load(uri).submit();
                            try {
                                stopIcons[key] = RouteStopIconBitmap(futureTarget.get())
                            } catch (e: Exception) {
                                result.error("-1", e.message, call.method)
                                return@launch
                            }
                        }
                    }
                    mpDirectionsRenderer?.setRoute(route, stopIcons)
                    
                    result.success("success")
                }
            }
            "setDefaultRouteStopIcon" -> {
                val icon = call.argument<String?>("icon")
                val uri = Uri.parse(icon)
                if (uri?.scheme == "mapsindoors") {
                    val routeStopIcon = gson.fromJson(uri.lastPathSegment, RouteStopIcon::class.java)
                    mpDirectionsRenderer?.setDefaultRouteStopIconConfig(routeStopIcon?.toMPRouteStopIconConfig(context))
                    result.success("success")
                } else if (uri?.scheme == "http" || uri?.scheme == "https") {
                    CoroutineScope(Dispatchers.Default).launch { 
                        val futureTarget: FutureTarget<Bitmap> = Glide.with(context).asBitmap().load(uri).submit();
                        try {
                            val icon = RouteStopIconBitmap(futureTarget.get())
                            mpDirectionsRenderer?.setDefaultRouteStopIconConfig(icon)
                            result.success("success")
                        } catch (e: Exception) {
                            result.error("-1", e.message, call.method)
                        }        
                    }
                } else {
                    result.error("-1", "Invalid icon uri", call.method)
                }
            }
            "useContentOfNearbyLocations" -> {
                result.success("success")
            }
            "showRouteLegButtons" -> {
                val show = call.argument<Boolean?>("show")
                show?.let {
                    mpDirectionsRenderer?.showRouteLegButtons(it)
                }
                result.success("success")
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    private fun applyOptions(options: DirectionsRendererOptions) {
        val renderer = mpDirectionsRenderer ?: return
        renderer.setConfig(options.toConfig())
        // animationRepeating is the one property with no slot on MPDirectionsRendererConfig, so it
        // has to go through setOptions. That call also rewrites the legacy two-line renderer's
        // colours and weights from the options object's defaults; the adapters prefer the config's
        // line style when there is one, so this only shows when neither the payload nor the CMS
        // sets a line style and the app relies on the deprecated setPolyLineColors.
        //
        // The same call sets mUseSpeedBasedAnimation, and sets it one way. Nothing here clears it and
        // neither does setConfig(null) from clearOptions - only the deprecated setAnimatedPolyline
        // does. So from the first payload that carries animationRepeating onwards, a duration set
        // through setAnimatedPolyline is ignored and the animation is timed from animationSpeed and
        // animationMinDuration instead, which the fallback below fills with the SDK's own defaults
        // whenever getOptions() is null. It only becomes visible when the effective config has no
        // animation block, which is exactly what an animationRepeating-only payload produces.
        options.animationRepeating?.let { repeating ->
            renderer.setOptions(
                (renderer.getOptions() ?: MPDirectionsRendererOptions())
                    .copy(animationRepeating = repeating)
            )
        }
    }

    fun setMapControl(mapControl: MapControl) {
        mMapControl = mapControl
        mpDirectionsRenderer = MPDirectionsRenderer(mapControl)
        // The renderer does not exist until now, so options set earlier were only cached.
        currentOptions?.let { applyOptions(it) }
    }
}