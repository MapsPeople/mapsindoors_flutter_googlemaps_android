package com.mapspeople.mapsindoors

import com.mapsindoors.core.models.MPIMapProviderBaseMapCache
import com.mapspeople.mapsindoors.core.BaseMapCacheProviderFactory

/**
 * Google Maps has no offline tile store, so there is nothing to cache base-map tiles into.
 *
 * [create] exists only to satisfy the compile-time seam the shared core resolves against, and is never reached: `DataSetCacheHandler` tests [isSupported] first and answers `MIError.BASEMAP_CACHE_NOT_SUPPORTED` - surfaced to Dart as `MPError.baseMapCachingNotSupported` - without constructing anything. It throws rather than returning `MPGoogleBaseMapCacheProvider`, so that a future caller which skips that check fails loudly instead of registering a provider nothing tests.
 */
object BaseMapCache : BaseMapCacheProviderFactory {
    override val isSupported = false

    override fun create(styleType: String, styleUri: String?): MPIMapProviderBaseMapCache =
        error("unreachable: BaseMapCache.isSupported is false on the Google Maps flavour")
}
