package com.jsh.pos.adapter.out.ai

import kotlin.test.Test
import kotlin.test.assertEquals

class GeminiModelResolverTest {

    @Test
    fun `flash tier uses supported default model`() {
        val actual = GeminiModelResolver.resolve(
            modelTier = "flash",
            flashModel = GeminiModelResolver.DEFAULT_FLASH_MODEL,
            proModel = GeminiModelResolver.DEFAULT_PRO_MODEL,
        )

        assertEquals("gemini-3-flash-preview", actual)
    }

    @Test
    fun `pro tier uses supported default model`() {
        val actual = GeminiModelResolver.resolve(
            modelTier = "pro",
            flashModel = GeminiModelResolver.DEFAULT_FLASH_MODEL,
            proModel = GeminiModelResolver.DEFAULT_PRO_MODEL,
        )

        assertEquals("gemini-3.1-pro-preview", actual)
    }

    @Test
    fun `configured model may include models prefix`() {
        val actual = GeminiModelResolver.resolve(
            modelTier = "flash",
            flashModel = "models/gemini-3-flash-preview",
            proModel = GeminiModelResolver.DEFAULT_PRO_MODEL,
        )

        assertEquals("gemini-3-flash-preview", actual)
    }

    @Test
    fun `blank configured model falls back to supported default`() {
        val actual = GeminiModelResolver.resolve(
            modelTier = "pro",
            flashModel = GeminiModelResolver.DEFAULT_FLASH_MODEL,
            proModel = "   ",
        )

        assertEquals("gemini-3.1-pro-preview", actual)
    }
}


