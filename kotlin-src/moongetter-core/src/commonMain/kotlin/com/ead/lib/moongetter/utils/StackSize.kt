package com.ead.lib.moongetter.utils

expect suspend fun <T> runWithStackSize(size: Int, block: () -> T): T