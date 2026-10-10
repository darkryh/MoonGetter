package com.ead.lib.moongetter.abyss
internal actual fun liveTarget(): String? = System.getenv("MOONGETTER_ABYSS_TEST_URL")
