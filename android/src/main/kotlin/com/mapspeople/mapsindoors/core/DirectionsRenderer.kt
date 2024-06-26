package com.mapspeople.mapsindoors.core

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.FutureTarget;
import com.mapsindoors.core.MPCameraViewFitMode
import com.mapsindoors.core.MPDirectionsRenderer
import com.mapsindoors.core.MPRoute
import com.mapsindoors.core.MapControl
import com.mapsindoors.core.MPRouteStopIconConfig
import com.mapsindoors.core.MPRouteStopIconProvider
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
    private var mpDirectionsRenderer: MPDirectionsRenderer? = null
    private var mMapControl: MapControl? = null

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
            "getSelectedLegFloorIndex" -> {
                var selectedLegFloorIndex = mpDirectionsRenderer?.selectedLegFloorIndex
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
                val gson = Gson()
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
                    val routeStopIcon = Gson().fromJson(uri.lastPathSegment, RouteStopIcon::class.java)
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

    fun setMapControl(mapControl: MapControl) {
        mMapControl = mapControl
        mpDirectionsRenderer = MPDirectionsRenderer(mapControl)
    }
}