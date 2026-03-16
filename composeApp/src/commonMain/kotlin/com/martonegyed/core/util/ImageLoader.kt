package com.martonegyed.core.util

import coil3.ImageLoader
import coil3.PlatformContext

expect fun getAsyncImageLoader(context: PlatformContext): ImageLoader