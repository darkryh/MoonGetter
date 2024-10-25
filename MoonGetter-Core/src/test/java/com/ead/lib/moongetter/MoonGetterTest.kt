package com.ead.lib.moongetter

import android.content.Context
import com.ead.lib.moongetter.models.Request
import org.junit.Before

class MoonGetterTest {

    private lateinit var moonGetterRequest : Request.Builder
    private lateinit var context: Context

    @Before
    fun setUp() {
        /*context = mockk(relaxed = true)
        moonGetterRequest = MoonGetter.initialize(context)*/
    }

    /*@Test(expected = InvalidServerException::class)
    fun `validating that when url is empty, an exception is thrown`() {
        runBlocking { moonGetterRequest.connect("").get() }
    }

    @Test(expected = InvalidServerException::class)
    fun `validating that when urls is empty, an exception is thrown`() {
        runBlocking { moonGetterRequest.connect(emptyList()).getList() }
    }*/
}