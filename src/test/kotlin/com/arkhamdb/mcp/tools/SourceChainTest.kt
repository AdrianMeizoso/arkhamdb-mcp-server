package com.arkhamdb.mcp.tools

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertNotNull

class SourceChainTest {

    @Test
    fun `primary sufficient skips fallbacks`() = runTest {
        var fallbackCalled = false
        val primary = SourceChain.Source("primary") { "A".repeat(200) }
        val fallback = SourceChain.Source("fallback") { fallbackCalled = true; "fallback result" }

        val result = SourceChain.primaryOrContrast(primary, listOf(fallback), minChars = 150)

        assertNotNull(result)
        assertEquals("A".repeat(200), result)
        assertEquals(false, fallbackCalled, "Fallback should not be called when primary is sufficient")
    }

    @Test
    fun `primary null triggers fallbacks`() = runTest {
        val primary = SourceChain.Source("primary") { null }
        val fallback = SourceChain.Source("fallback") { "fallback content" }

        val result = SourceChain.primaryOrContrast(primary, listOf(fallback))

        assertEquals("fallback content", result)
    }

    @Test
    fun `primary blank triggers fallbacks`() = runTest {
        val primary = SourceChain.Source("primary") { "  " }
        val fallback = SourceChain.Source("fallback") { "fallback content" }

        val result = SourceChain.primaryOrContrast(primary, listOf(fallback))

        assertEquals("fallback content", result)
    }

    @Test
    fun `primary insufficient triggers fallbacks and merges`() = runTest {
        val primary = SourceChain.Source("primary") { "short" }
        val fallback = SourceChain.Source("fallback") { "fallback content" }

        val result = SourceChain.primaryOrContrast(primary, listOf(fallback), minChars = 150)

        assertNotNull(result)
        assertNotNull(result.contains("short"), "Should include primary partial output")
        assertNotNull(result.contains("fallback content"), "Should include fallback output")
    }

    @Test
    fun `all null returns null`() = runTest {
        val primary = SourceChain.Source("primary") { null }
        val fallback1 = SourceChain.Source("f1") { null }
        val fallback2 = SourceChain.Source("f2") { null }

        val result = SourceChain.primaryOrContrast(primary, listOf(fallback1, fallback2))

        assertNull(result)
    }

    @Test
    fun `mergeAll concatenates all non-null sources`() = runTest {
        val sources = listOf(
            SourceChain.Source("s1") { "result one" },
            SourceChain.Source("s2") { null },
            SourceChain.Source("s3") { "result three" }
        )

        val result = SourceChain.mergeAll(sources)

        assertNotNull(result)
        assertNotNull(result.contains("result one"))
        assertNotNull(result.contains("result three"))
    }

    @Test
    fun `mergeAll all null returns null`() = runTest {
        val sources = listOf(
            SourceChain.Source("s1") { null },
            SourceChain.Source("s2") { null }
        )

        val result = SourceChain.mergeAll(sources)

        assertNull(result)
    }
}
