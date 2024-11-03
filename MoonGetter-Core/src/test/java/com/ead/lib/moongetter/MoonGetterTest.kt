package com.ead.lib.moongetter

import android.content.Context
import com.ead.lib.moongetter.models.builder.Engine
import com.ead.lib.moongetter.models.builder.Factory
import com.ead.lib.moongetter.models.exceptions.InvalidServerException
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test

class MoonGetterTest {

    private lateinit var moonGetterFactory : Factory.Builder
    private lateinit var context: Context

    private val fakeEngine = Engine.Builder().build()

    @Before
    fun setUp() {
        context = mockk(relaxed = true)
        moonGetterFactory = MoonGetter
            .initialize(context)
            .setEngine(fakeEngine)
    }

    @Test(expected = InvalidServerException::class)
    fun `validating that when url is empty, an exception is thrown`() {
        runBlocking { moonGetterFactory.get("") }
    }

    @Test(expected = InvalidServerException::class)
    fun `validating that when urls is empty, an exception is thrown`() {
        runBlocking { moonGetterFactory.get(emptyList()) }
    }
}