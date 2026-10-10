package com.ead.lib.moongetter.abyss

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import platform.posix.getenv

@OptIn(ExperimentalForeignApi::class)
internal actual fun liveTarget(): String? = getenv("MOONGETTER_ABYSS_TEST_URL")?.toKString()
