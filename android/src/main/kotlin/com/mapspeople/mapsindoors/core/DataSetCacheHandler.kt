package com.mapspeople.mapsindoors.core

import android.os.Handler
import android.os.Looper
import com.google.gson.Gson
import com.mapsindoors.core.MPBaseMapCacheRegionListener
import com.mapsindoors.core.MPDataSetCacheManager
import com.mapsindoors.core.MPDataSetCacheScope
import com.mapsindoors.core.MapsIndoors
import com.mapsindoors.core.errors.MIError
import com.mapsindoors.core.models.MPIMapProviderBaseMapCache
import com.mapspeople.mapsindoors.BaseMapCache
import com.mapspeople.mapsindoors.core.models.MPError
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler

/**
 * Bridges the MapsIndoors base-map tile cache onto `DataSetCacheMethodChannel`.
 *
 * Base-map tiles are the map provider's own tiles, underneath MapsIndoors. They are deliberately independent of the MapsIndoors content cache, so a slow tile download never delays a dataset sync.
 *
 * Registration is deferred until a MapsIndoors map view exists, matching iOS, where the map provider registers its cache implementation when the map view is created. Enabling before then reports `MIError.BASEMAP_CACHE_NOT_REGISTERED`.
 */
class DataSetCacheHandler(messenger: BinaryMessenger, private val getMapView: () -> MapView?) : MethodCallHandler {
    private val dataSetCacheChannel = MethodChannel(messenger, "DataSetCacheMethodChannel")
    private val gson = Gson()

    /** Both `MPBaseMapCacheRegionListener` callbacks are `@AnyThread`, and a `Result` may only be completed from the platform thread. */
    private val mainHandler = Handler(Looper.getMainLooper())

    private var provider: MPIMapProviderBaseMapCache? = null

    private var pendingSyncResult: MethodChannel.Result? = null

    init {
        dataSetCacheChannel.setMethodCallHandler(this)
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
    }

    private fun errorJson(code: Int, message: String): String =
        gson.toJson(MPError.fromMIError(MIError(code, message)))

    /** Completes the pending synchronization exactly once, so a second callback cannot crash the engine. */
    private fun completeSync(payload: String?) {
        val result = pendingSyncResult ?: return
        pendingSyncResult = null
        result.success(payload)
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        fun <T> arg(name: String): T? = call.argument<T>(name)

        when (call.method.removePrefix("DSC_")) {
            "isBaseMapCachingSupported" -> {
                result.success(BaseMapCache.isSupported)
            }
            "enableBaseMapCaching" -> {
                enableBaseMapCaching(arg<String>("styleSource"), arg<String>("scope"), result)
            }
            "synchronizeBaseMapTiles" -> {
                synchronizeBaseMapTiles(result)
            }
            // Without this a method this handler does not know returns without completing the Result,
            // and the Dart future waits forever instead of failing.
            else -> result.notImplemented()
        }
    }

    private fun enableBaseMapCaching(styleSourceJson: String?, scopeName: String?, result: MethodChannel.Result) {
        // Checked before anything else: on the Google flavour the answer never depends on state.
        if (!BaseMapCache.isSupported) {
            result.success(errorJson(MIError.BASEMAP_CACHE_NOT_SUPPORTED,
                "The Google Maps provider cannot cache base-map tiles"))
            return
        }

        if (getMapView() == null) {
            result.success(errorJson(MIError.BASEMAP_CACHE_NOT_REGISTERED,
                "Base-map tile caching needs a MapsIndoors map view. Build a MapsIndoorsWidget before calling enableBaseMapCaching"))
            return
        }

        val apiKey = MapsIndoors.getAPIKey()
        if (apiKey.isNullOrEmpty()) {
            result.success(errorJson(MIError.SDK_NOT_INITIALIZED,
                "No API key is loaded. Call loadMapsIndoors before enableBaseMapCaching"))
            return
        }

        val styleSource = if (styleSourceJson != null) gson.fromJson(styleSourceJson, StyleSource::class.java) else null
        val scope = try {
            MPDataSetCacheScope.valueOf(scopeName ?: MPDataSetCacheScope.FULL.name)
        } catch (e: IllegalArgumentException) {
            result.error("-1", "Unknown caching scope $scopeName", null)
            return
        }

        cancelActiveProvider("Base-map tile synchronization was cancelled because base-map caching was re-enabled")
        provider = BaseMapCache.create(styleSource?.type ?: "mapsIndoorsDefault", styleSource?.styleUri)

        val manager = MPDataSetCacheManager.getInstance()
        manager.setBaseMapCacheProvider(provider)

        // addDataSetWithCachingScope returns an already-managed dataset untouched, ignoring both the
        // flag and the scope, so an existing one has to be flagged through its own setter instead.
        val existing = manager.getDataSetByID(apiKey)
        if (existing != null) {
            existing.setBaseMapTilesEnabled(true)
        } else {
            manager.addDataSetWithCachingScope(apiKey, null, scope, true)
        }

        result.success(null)
    }

    private fun synchronizeBaseMapTiles(result: MethodChannel.Result) {
        // Checked before the provider, and in the same order as iOS. On the Google flavour
        // enableBaseMapCaching reports 9000 without registering a provider, so testing the provider first
        // would answer 9001 here - a different code than iOS gives for the same condition.
        if (!BaseMapCache.isSupported) {
            result.success(errorJson(MIError.BASEMAP_CACHE_NOT_SUPPORTED,
                "The Google Maps provider cannot cache base-map tiles"))
            return
        }

        if (provider == null) {
            result.success(errorJson(MIError.BASEMAP_CACHE_NOT_REGISTERED,
                "Base-map tile caching is not enabled. Call enableBaseMapCaching first"))
            return
        }

        if (pendingSyncResult != null) {
            result.error("-1", "A base-map tile synchronization is already running", null)
            return
        }

        pendingSyncResult = result

        MPDataSetCacheManager.getInstance().synchronizeBaseMapTiles(object : MPBaseMapCacheRegionListener {
            override fun onProgress(fraction: Double) {
                runOnMain {
                    dataSetCacheChannel.invokeMethod("onBaseMapCacheProgress", mapOf("fraction" to fraction))
                }
            }

            override fun onComplete(error: MIError?) {
                runOnMain {
                    completeSync(if (error == null) null else gson.toJson(MPError.fromMIError(error)))
                }
            }
        })
    }

    /**
     * Unregisters the active provider, cancelling any download it owns.
     *
     * A run the manager owns is settled by the manager, not here: `setBaseMapCacheProvider` abandons every run not using the newly registered provider, and abandoning one delivers `onComplete` synchronously on the calling thread. This runs on the platform thread, so `runOnMain` executes inline and the SDK's completion reaches [completeSync] before the [completeSync] call below is reached. What a caller sees on re-enable or destroy is therefore the SDK's own `MIError.UNKNOWN_ERROR` about the registered provider having been replaced, rather than the [reason] passed in here.
     *
     * That call is kept as a fallback, and is idempotent. It covers what the manager cannot settle: a pending result with no run the manager owns, which would otherwise leave [pendingSyncResult] set and refuse every later sync.
     *
     * Does nothing when no provider was ever created, which also keeps `MPDataSetCacheManager.getInstance()` off the teardown path: it throws `IllegalStateException` until `MapsIndoors.load` has initialised it, and teardown runs for every app whether or not it ever cached anything.
     */
    private fun cancelActiveProvider(reason: String) {
        val active = provider ?: return
        provider = null
        active.terminate()
        MPDataSetCacheManager.getInstance().setBaseMapCacheProvider(null)
        runOnMain { completeSync(errorJson(MIError.UNKNOWN_ERROR, reason)) }
    }

    fun terminate() {
        cancelActiveProvider("Base-map tile synchronization was cancelled because MapsIndoors was destroyed")
    }

    fun dispose() {
        terminate()
        dataSetCacheChannel.setMethodCallHandler(null)
    }

    /** Mirrors the Dart `MPMapboxStyleSource` payload. */
    private data class StyleSource(val type: String?, val styleUri: String?)
}
