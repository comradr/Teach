package com.example

import org.junit.Assert.assertEquals
import org.junit.Test

class GeminiApiKeyProviderTest {
    @Test
    fun localKeyHasPriorityOverBuildKey() {
        assertEquals(
            "local-personal-key",
            GeminiApiKeyProvider.resolve("  local-personal-key  ", "build-key")
        )
    }

    @Test
    fun invalidLocalKeyFallsBackToBuildKey() {
        assertEquals(
            "build-key",
            GeminiApiKeyProvider.resolve("MY_GEMINI_API_KEY", "  build-key  ")
        )
    }
}
