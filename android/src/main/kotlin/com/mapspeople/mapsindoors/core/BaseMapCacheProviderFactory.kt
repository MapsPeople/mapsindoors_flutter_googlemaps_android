package com.mapspeople.mapsindoors.core

import com.mapsindoors.core.models.MPIMapProviderBaseMapCache

/**
 * Per-provider access to the map provider's base-map tile cache implementation.
 *
 * Resolved at compile time: each provider package declares its own `com.mapspeople.mapsindoors.BaseMapCache`, the same seam `PlatformMapView` and `Util` already use, so the shared core reaches the Mapbox or the Google implementation without either needing to be on the other's classpath.
 */
interface BaseMapCacheProviderFactory {
    /** Whether this map provider can cache base-map tiles at all. */
    val isSupported: Boolean

    /**
     * Builds a provider that caches the given style.
     *
     * [styleType] is either "mapsIndoorsDefault" or "custom", in which case [styleUri] carries the Mapbox style URI.
     */
    fun create(styleType: String, styleUri: String?): MPIMapProviderBaseMapCache
}
