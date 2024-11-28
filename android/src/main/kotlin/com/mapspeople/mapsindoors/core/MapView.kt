package com.mapspeople.mapsindoors.core

import android.graphics.Typeface
import android.content.Context
import android.view.View
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.gson.Gson
import com.mapsindoors.core.*
import com.mapsindoors.core.models.MPCameraEventListener
import com.mapsindoors.core.models.MPCameraPosition
import com.mapsindoors.core.models.MPOnInfoWindowClickListener
import com.mapsindoors.core.models.MPOnMarkerClickListener
import com.mapsindoors.core.models.MPMapStyle
import com.mapspeople.mapsindoors.core.models.*
import com.mapspeople.mapsindoors.core.*
import com.mapspeople.mapsindoors.PlatformMapView
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.platform.PlatformView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MapView(context: Context, binaryMessenger: BinaryMessenger, val args: HashMap<*,*>?, private val lifecycleProvider: MapsindoorsPlugin.LifecycleProvider) : PlatformMapView(context, args), PlatformView, MethodCallHandler, DefaultLifecycleObserver {
    private val channel : MethodChannel = MethodChannel(binaryMessenger, "MapControlMethodChannel")
    private val listenerChannel : MethodChannel = MethodChannel(binaryMessenger, "MapControlListenerMethodChannel")
    private val floorSelectorChannel : MethodChannel = MethodChannel(binaryMessenger, "MapControlFloorSelectorChannel")
    private val gson = Gson()
    private val mDirectionsRenderer: DirectionsRenderer = DirectionsRenderer(context, binaryMessenger)
    private var mConfig: MapConfig? = null
    private var initializing: Boolean = false
    var mapControl : MapControl? = null

    init {
        lifecycleProvider.getLifecycle()?.addObserver(this)
        mConfig = gson.fromJson(args?.get("mapConfig") as? String, MapConfig::class.java)
        channel.setMethodCallHandler(this::onMethodCall)
        listenerChannel.setMethodCallHandler(this::onListenerCall)
        floorSelectorChannel.setMethodCallHandler { call, result ->
            when (call.method) {
                "FSE_onFloorChanged" -> {
                    floorSelectorInterface.listener?.onFloorSelectionChanged(gson.fromJson(call.argument<String>("floor"), MPFloor::class.java))
                }
                else -> result.notImplemented()
            }
        }
    }

    override fun getView(): View {
        return getMapView()
    }

    override fun dispose() {
        lifecycleProvider.getLifecycle()?.removeObserver(this)
        disposeMap()
        mapControl?.onDestroy()
        mapControl = null
        mConfig = null
    }

    override fun whenMapReady() {
        args?.get("floorSelectorAutoFloorChange")?.let {
            floorSelectorInterface.autoFloorChange = it as Boolean
        }
        initialize()
    }

    private val floorSelectorInterface = object : MPFloorSelectorInterface {
        var autoFloorChange = true
        var listener : OnFloorSelectionChangedListener? = null
        override fun getView(): View? {
            return null
        }

        override fun setOnFloorSelectionChangedListener(p0: OnFloorSelectionChangedListener?) {
            listener = p0
        }

        override fun setList(p0: MutableList<MPFloor>?) {
            val ret = if (p0 == null) null else gson.toJson(p0)
            floorSelectorChannel.invokeMethod("setList", ret)
        }

        override fun show(p0: Boolean, p1: Boolean) {
            floorSelectorChannel.invokeMethod("show", mapOf("show" to p0, "animate" to p1))
        }

        override fun setSelectedFloor(p0: MPFloor) {
            floorSelectorChannel.invokeMethod("setSelectedFloor", gson.toJson(p0))
        }

        override fun setSelectedFloorByZIndex(p0: Int) {
            floorSelectorChannel.invokeMethod("setSelectedFloorByZIndex", p0)
        }

        override fun zoomLevelChanged(p0: Float) {
            floorSelectorChannel.invokeMethod("zoomLevelChanged", p0.toDouble())
        }

        override fun isAutoFloorChangeEnabled(): Boolean {
            return autoFloorChange
        }

        override fun setUserPositionFloor(p0: Int) {
            floorSelectorChannel.invokeMethod("setUserPositionFloor", p0)
        }
    }
 
    public fun initialize() {
        if (!initializing) {
            initializing = true;
            mapControl?.let {
                it.onDestroy()
                mapControl = null
            }
            CoroutineScope(Dispatchers.Main).launch {
                makeMPConfig(mConfig, floorSelectorInterface)?.let {
                    MapControl.create(it) { mc, e ->
                        if (e == null && mc != null) {
                            mDirectionsRenderer.setMapControl(mc)
                            mapControl = mc
                            setupListeners()
                            channel.invokeMethod("create", gson.toJson(e))
                        }
                        initializing = false
                    }
                }
            }
        }
    }

    private var cameraListener : MPCameraEventListener? = null
    private var floorUpdateListener : OnFloorUpdateListener? = null
    private var buildingFoundAtCameraTargetListener : OnBuildingFoundAtCameraTargetListener? = null
    private var venueFoundAtCameraTargetListener : OnVenueFoundAtCameraTargetListener? = null
    private var locationSelectedListener : OnLocationSelectedListener? = null
    private var mapClickListener : MPOnMapClickListener? = null
    private var markerClickListener : MPOnMarkerClickListener? = null
    private var markerInfoWindowClickListener : MPOnInfoWindowClickListener? = null

    private fun setupListeners() {
        cameraListener?.let {
            mapControl?.addOnCameraEventListener(it)
        }
        floorUpdateListener?.let {
            mapControl?.addOnFloorUpdateListener(it)
        }
        mapControl?.setOnCurrentBuildingChangedListener(buildingFoundAtCameraTargetListener)
        mapControl?.setOnCurrentVenueChangedListener(venueFoundAtCameraTargetListener)
        mapControl?.setOnLocationSelectedListener(locationSelectedListener)
        mapControl?.setOnMapClickListener(mapClickListener)
        mapControl?.setOnMarkerClickListener(markerClickListener)
        mapControl?.setOnMarkerInfoWindowClickListener(markerInfoWindowClickListener)
    }

    private fun setupFloorSelector(autoFloorChangeEnabled: Boolean?) {
        autoFloorChangeEnabled?.let {
            floorSelectorInterface.autoFloorChange  = it
        }
        mapControl?.floorSelector = floorSelectorInterface
        mapControl?.hideFloorSelector(false)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        fun error(code: String = "-1", message: String? = "Argument was null", details: Any? = null) = result.error(code, message, details)

        fun success(ret : Any? = "success") = result.success(ret)

        fun <T> arg(name: String) : T? = call.argument<T>(name)

        val method = call.method.drop(4)

        when (method) {
            "selectFloor" -> {
                val floor : Int = arg<Int>("floorIndex") as Int
                try {
                    mapControl?.selectFloor(floor)
                    success()
                } catch (e: Exception) {
                    error("-1", e.message, call.method)
                }
            }
            "clearFilter" -> {
                mapControl?.clearFilter()
                success()
            }
            "deSelectLocation" -> {
                mapControl?.deSelectLocation()
                success()
            }
            "getCurrentBuilding" -> {
                success(gson.toJson(mapControl?.currentBuilding))
            }
            "getCurrentBuildingFloor" -> {
                success(gson.toJson(mapControl?.currentBuildingFloor))
            }
            "getCurrentFloorIndex" -> {
                success(mapControl?.currentFloorIndex)
            }
            "setFloorSelector" -> {
                setupFloorSelector(arg<Boolean>("isAutoFloorChangeEnabled"))
                success()
            }
            "getCurrentMapsIndoorsZoom" -> {
                success(mapControl?.currentMapsIndoorsZoom)
            }
            "getCurrentVenue" -> {
                success(gson.toJson(mapControl?.currentVenue))
            }
            "getMapStyle" -> {
                success(gson.toJson(mapControl?.mapStyle))
            }
            "getMapViewPaddingBottom" -> {
                success(mapControl?.mapViewPaddingBottom)
            }
            "getMapViewPaddingEnd" -> {
                success(mapControl?.mapViewPaddingEnd)
            }
            "getMapViewPaddingStart" -> {
                success(mapControl?.mapViewPaddingStart)
            }
            "getMapViewPaddingTop" -> {
                success(mapControl?.mapViewPaddingTop)
            }
            "goTo" -> {
                val json: String? = arg<String>("entity")
                if (json == null) {
                    success()
                    return
                }
                val entity : MPEntity?
                try {
                    when (arg<String>("type")) {
                        "MPLocation" -> {
                            entity = gson.fromJson(json, Location::class.java).toMPLocation()
                        }
                        "MPFloor" -> {
                            entity = gson.fromJson(json, MPFloor::class.java)
                        }
                        "MPBuilding" -> {
                            entity = gson.fromJson(json, MPBuilding::class.java)
                        }
                        "MPVenue" -> {
                            entity = gson.fromJson(json, MPVenue::class.java)
                        }
                        else -> {
                            result.error("-1", "Not a mapsIndoors entity", call.method)
                            return
                        }
                    }
                } catch (e: Exception) {
                    error("-1", e.message)
                    return
                }
                mapControl?.goTo(entity)
                success()
            }
            "hideFloorSelector" -> {
                mapControl?.hideFloorSelector(arg<Boolean>("hide") as Boolean)
                success()
            }
            "isFloorSelectorHidden" -> {
                success(mapControl?.isFloorSelectorHidden)
            }
            "isUserPositionShown" -> {
                success(mapControl?.isUserPositionShown)
            }
            "selectBuilding" -> {
                val building = gson.fromJson(arg("building") as String?, MPBuilding::class.java)
                val moveCamera = arg<Boolean>("moveCamera")
                if (building != null && moveCamera != null) {
                    mapControl?.selectBuilding(building, moveCamera)
                    success()
                } else {
                    error("-1", "parameters are null", call.method)
                }
            }
            "selectLocation" -> {
                val location = MapsIndoors.getLocationById(arg<String>("location"))
                val behavior = gson.fromJson(arg("behavior") as String?, MapBehavior::class.java)
                if (behavior != null) {
                    mapControl?.selectLocation(location, behavior.toMPSelectionBehavior())
                    success()
                } else {
                    error("-1", "behavior is null", call.method)
                }
            }
            "selectLocationById" -> {
                val id = arg<String>("id")
                val behavior = gson.fromJson(arg<String>("behavior"), MapBehavior::class.java)
                if (id != null && behavior != null) {
                    mapControl?.selectLocation(id, behavior.toMPSelectionBehavior())
                    success("success")
                } else {
                    error("-1", "parameters are null", call.method)
                }
            }
            "selectVenue" -> {
                val venue = gson.fromJson(arg("venue") as String?, MPVenue::class.java)
                val moveCamera = arg<Boolean>("moveCamera")
                if (venue != null && moveCamera != null) {
                    mapControl?.selectVenue(venue, moveCamera)
                    success("success")
                } else {
                    error("-1", "parameters are null", call.method)
                }
            }
            "setFilter" -> {
                val filter = gson.fromJson(arg<String>("filter"), Filter::class.java)
                val behavior = gson.fromJson(arg<String>("behavior"), MapBehavior::class.java)
                if (filter != null && behavior != null) {
                    mapControl?.setFilter(filter.toMPFilter(), behavior.toMPFilterBehavior(), object : MPSuccessListener {
                        override fun onSuccess() {
                            success()
                        }
                        override fun onFailure() {
                            error("-1", "could not set filter", null)
                        }
                    })
                } else {
                    error("-1", "parameters are null", call.method)
                }
            }
            "setFilterWithLocations" -> {
                val locationIds = arg<List<String>>("locations") as List<String>?
                val locations : MutableList<MPLocation> = mutableListOf()
                for (id in locationIds!!){
                    locations.add(MapsIndoors.getLocationById(id)!!)
                }
                val behavior = gson.fromJson(arg<String>("behavior"), MapBehavior::class.java)
                if (locations != null && behavior != null) {
                    mapControl?.setFilter(locations, behavior.toMPFilterBehavior())
                    success()
                } else {
                    error("-1", "parameters are null", call.method)
                }
            }
            "setMapPadding" -> {
                val start = arg<Int>("start")
                val top = arg<Int>("top")
                val end = arg<Int>("end")
                val bottom = arg<Int>("bottom")
                if (start != null && top != null && end != null && bottom != null) {
                    mapControl?.setMapPadding(start, top, end, bottom)
                    success()
                } else {
                    error("-1", "Some arguements were null", call.method)
                }
            }
            "setMapStyle" -> {
                val mapStyle = gson.fromJson(arg<String>("mapStyle"), MPMapStyle::class.java)
                if (mapStyle != null) {
                    mapControl?.mapStyle = mapStyle
                    success()
                } else {
                    error("-1", "some arguments were null", call.method)
                }
            }
            "showInfoWindowOnClickedLocation" -> {
                val should = arg<Boolean>("show")
                if (should != null) {
                    mapControl?.showInfoWindowOnClickedLocation(should)
                    success()
                } else {
                    error("-1", "some arguments were null", call.method)
                }
            }
            "showUserPosition" -> {
                val should = arg<Boolean>("show")
                if (should != null) {
                    mapControl?.showUserPosition(should)
                    success()
                } else {
                    error("-1", "some arguments were null", call.method)
                }
            }
            "enableLiveData" -> {
                val hasListener = arg<Boolean>("listener")
                val domainType = arg<String>("domainType")
                if (hasListener == true) {
                    val listener = OnLiveLocationUpdateListener { 
                        listenerChannel.invokeMethod("onLiveLocationUpdate", mapOf("location" to it, "domainType" to domainType))
                    }
                    mapControl?.enableLiveData(domainType, listener)
                } else {
                    mapControl?.enableLiveData(domainType)
                }
                success()
            }
            "disableLiveData" -> {
                val domainType = arg<String>("domainType")
                if (domainType != null) {
                    mapControl?.disableLiveData(domainType)
                }
                success()
            }
            "moveCamera", "animateCamera" -> {
                val update = gson.fromJson<CameraUpdate>(arg<String>("update"), CameraUpdate::class.java)
                val duration : Int? = arg<Int>("duration")


                updateCamera(call.method == "moveCamera", update, duration) {
                    success()
                }
            }
            "getCurrentCameraPosition" -> {
                success(gson.toJson(currentCameraPosition))
            }
            "setLabelOptions" -> {
                val textSize = arg<Int?>("textSize")
                val color = arg<String?>("color")
                val showHalo = arg<Boolean>("showHalo")
                mapControl?.apply {
                    setMapLabelFont(Typeface.DEFAULT, color, showHalo!!)
                    if (textSize != null) {
                        setMapLabelTextSize(15)
                    }
                }
                success()
            }
            "setHiddenFeatures" -> {
                val features = arg<List<Int>?>("features")
                val list = features?.map {
                    when(it) {
                        0 -> MPFeatureType.MODEL_2D
                        1 -> MPFeatureType.WALLS_2D
                        2 -> MPFeatureType.MODEL_2D
                        3 -> MPFeatureType.WALLS_3D
                        4 -> MPFeatureType.EXTRUSION_3D
                        5 -> MPFeatureType.EXTRUDED_BUILDINGS
                        else -> MPFeatureType.EXTRUDED_BUILDINGS
                    }
                }
                mapControl?.setHiddenFeatures(list)
                success()
            }
            "getHiddenFeatures" -> {
                val features = mapControl?.getHiddenFeatures()
                val list: List<Int>? = features?.map {
                    when(it) {
                        MPFeatureType.MODEL_2D -> 0
                        MPFeatureType.WALLS_2D -> 1
                        MPFeatureType.MODEL_2D -> 2
                        MPFeatureType.WALLS_3D -> 3
                        MPFeatureType.EXTRUSION_3D -> 4
                        MPFeatureType.EXTRUDED_BUILDINGS -> 5
                        else -> 5
                    }
                }
                success(list)
            }
            "clearHighlight" -> {
                mapControl?.clearHighlight()
                success()
            }
            "setHighlight" -> {
                val locationIds = arg<List<String>>("locations") as List<String>?
                val locations : MutableList<MPLocation> = mutableListOf()
                for (id in locationIds!!){
                    locations.add(MapsIndoors.getLocationById(id)!!)
                }
                val behavior = gson.fromJson(arg<String>("behavior"), MapBehavior::class.java)
                if (locations != null && behavior != null) {
                    mapControl?.setHighlight(locations, behavior.toMPHighlightBehavior())
                    success()
                } else {
                    error("-1", "parameters are null", call.method)
                }
            }
            "setBuildingSelectionMode" -> {
                val mode = arg<Int>("mode")
                mapControl?.buildingSelectionMode = MPSelectionMode.values()[mode!!]
                success()
            }
            "getBuildingSelectionMode" -> {
                success(mapControl?.buildingSelectionMode?.ordinal)
            }
            "setFloorSelectionMode" -> {
                val mode = arg<Int>("mode")
                mapControl?.floorSelectionMode = MPSelectionMode.values()[mode!!]
                success()
            }
            "getFloorSelectionMode" -> {
                success(mapControl?.floorSelectionMode?.ordinal)
            }
            "refresh" -> {
                mapControl?.refresh()
                success()
            }
            "showCompassOnRotate" -> {
                val should = arg<Boolean>("show")
                if (should != null) {
                    // TODO: Implement this
                    // mMapControl?.showCompassOnRotate(should)
                    success()
                } else {
                    error("-1", "some arguments were null", call.method)
                }
            }
            else -> {
                result.notImplemented()
            }
        }
    }

    private fun onListenerCall(call: MethodCall, result: MethodChannel.Result) {
        val setup = call.argument<Boolean?>("setupListener")
        if (setup == null) {
            result.error("-1", "Cannot modify listener when setup is $setup", null)
            return
        }
        val consumeEvent = call.argument<Boolean?>("consumeEvent")

        val method = call.method.drop(4)

        when (method) {
            "cameraEventListener" -> {
                if (setup) {
                    cameraListener = MPCameraEventListener {
                        listenerChannel.invokeMethod("onCameraEvent", it.ordinal)
                    }.also {
                        mapControl?.addOnCameraEventListener(it)
                    }
                } else {
                    cameraListener?.let {
                        mapControl?.removeOnCameraEventListener(it)
                    }
                    cameraListener = null
                }
            }
            "floorUpdateListener" -> {
                if (setup) {
                    floorUpdateListener = OnFloorUpdateListener { _, floor ->
                        listenerChannel.invokeMethod("onFloorUpdate", floor)
                    }.also {
                        mapControl?.addOnFloorUpdateListener(it)
                    }
                } else {
                    floorUpdateListener?.let {
                        mapControl?.removeOnFloorUpdateListener(it)
                    }
                    floorUpdateListener = null
                }
            }
            "buildingFoundAtCameraTargetListener" -> {
                if (setup) {
                    buildingFoundAtCameraTargetListener = OnBuildingFoundAtCameraTargetListener {
                        listenerChannel.invokeMethod("onBuildingFoundAtCameraTarget", gson.toJson(it))
                    }.also {
                        mapControl?.setOnCurrentBuildingChangedListener(it)
                    }
                } else {    
                    mapControl?.setOnCurrentBuildingChangedListener(null)
                    buildingFoundAtCameraTargetListener = null
                }
            }
            "venueFoundAtCameraTargetListener" -> {
                if (setup) {
                    venueFoundAtCameraTargetListener = OnVenueFoundAtCameraTargetListener {
                        listenerChannel.invokeMethod("onVenueFoundAtCameraTarget", gson.toJson(it))
                    }.also {
                        mapControl?.setOnCurrentVenueChangedListener(it)
                    }
                } else {
                    mapControl?.setOnCurrentVenueChangedListener(null)
                    venueFoundAtCameraTargetListener = null
                }
            }
            "locationSelectedListener" -> {
                if (setup) {
                    locationSelectedListener = OnLocationSelectedListener {
                        listenerChannel.invokeMethod("onLocationSelected", gson.toJson(it))
                        return@OnLocationSelectedListener consumeEvent == true
                    }.also {
                        mapControl?.setOnLocationSelectedListener(it)
                    }
                } else {
                    mapControl?.setOnLocationSelectedListener(null)
                    locationSelectedListener = null
                }
            }
            "mapClickListener" -> {
                if (setup) {
                    mapClickListener = MPOnMapClickListener { latLng, locations ->
                        val point = gson.toJson(MPPoint(latLng))
                        val locs = gson.toJson(locations)
                        CoroutineScope(Dispatchers.Main).launch {
                            listenerChannel.invokeMethod("onMapClick", mapOf("locations" to locs, "point" to point))
                        }
                        return@MPOnMapClickListener consumeEvent == true
                    }.also {
                        mapControl?.setOnMapClickListener(it)
                    }
                } else {
                    mapControl?.setOnMapClickListener(null)
                    mapClickListener = null
                }
            }
            "markerClickListener" -> {
                if (setup) {
                    markerClickListener = MPOnMarkerClickListener {
                        listenerChannel.invokeMethod("onMarkerClick", it?.id)
                        return@MPOnMarkerClickListener consumeEvent == true
                    }.also {
                        mapControl?.setOnMarkerClickListener(it)
                    }
                } else {
                    mapControl?.setOnMarkerClickListener(null)
                    markerClickListener = null
                }
            }
            "markerInfoWindowClickListener" -> {
                if (setup) {
                    markerInfoWindowClickListener = MPOnInfoWindowClickListener {
                        listenerChannel.invokeMethod("onInfoWindowClick", it?.id)
                    }.also {
                        mapControl?.setOnMarkerInfoWindowClickListener(it)
                    }
                } else {
                    mapControl?.setOnMarkerInfoWindowClickListener(null)
                    markerInfoWindowClickListener = null
                }
            }
        }
    }
}