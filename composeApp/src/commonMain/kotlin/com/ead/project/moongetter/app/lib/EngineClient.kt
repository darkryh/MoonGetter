package com.ead.project.moongetter.app.lib

import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory


expect val httpClientEngineFactory : HttpClientEngineFactory<HttpClientEngineConfig>