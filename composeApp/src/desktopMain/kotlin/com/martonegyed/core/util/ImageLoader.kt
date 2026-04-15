package com.martonegyed.core.util

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade
import coil3.util.DebugLogger
import okio.Path.Companion.toOkioPath
import java.io.File

actual fun getAsyncImageLoader(context: PlatformContext): ImageLoader {
    val cacheDir = File(System.getProperty("user.home"), ".cinegraph/image_cache").apply {
        mkdirs()
    }

    return ImageLoader.Builder(context)
        .components {
            add(KtorNetworkFetcherFactory())
        }
        .memoryCache {
            MemoryCache.Builder()
                .maxSizePercent(context, 0.25)
                .build()
        }
        .diskCache {
            DiskCache.Builder()
                .directory(cacheDir.toOkioPath())
                .maxSizeBytes(50L * 1024 * 1024)
                .build()
        }
        .logger(DebugLogger())
        .crossfade(true)
        .build()
}