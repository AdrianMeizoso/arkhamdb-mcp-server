package com.arkhamdb.mcp.tools

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertTrue

class PdfCacheSearchTest {

    private fun makeText(vararg lines: String): String = lines.joinToString("\n")

    @Test
    fun `exact phrase match finds correct line`() {
        val text = makeText(
            "Line one about nothing",
            "Line two about evasión de enemigo",
            "Line three about other things"
        )
        val result = PdfCache.search(text, "evasión de enemigo")
        assertContains(result, "evasión de enemigo")
    }

    @Test
    fun `AND fallback when exact phrase fails`() {
        val text = makeText(
            "Line one about nothing",
            "This line has both palabras and clave",
            "Line three is irrelevant"
        )
        val result = PdfCache.search(text, "palabras clave")
        assertContains(result, "palabras")
        assertContains(result, "clave")
    }

    @Test
    fun `OR fallback when AND fails`() {
        val text = makeText(
            "This line mentions acción",
            "This line mentions something else",
            "This line mentions reacción"
        )
        val result = PdfCache.search(text, "acción reacción")
        assertTrue(!result.startsWith("No results"))
    }

    @Test
    fun `no match returns no results message`() {
        val text = makeText(
            "Line one",
            "Line two",
            "Line three"
        )
        val result = PdfCache.search(text, "zzznomatch")
        assertTrue(result.startsWith("No results"))
    }

    @Test
    fun `context window includes surrounding lines`() {
        val lines = (1..20).map { "Line $it content" }
        val text = lines.joinToString("\n")
        // Match is at line 10 (index 9), window of 2 should include lines 8-12
        val result = PdfCache.search(text, "Line 10 content", contextWindow = 2)
        assertContains(result, "Line 8 content")
        assertContains(result, "Line 10 content")
        assertContains(result, "Line 12 content")
    }

    @Test
    fun `overlapping windows are merged`() {
        val lines = (1..10).map { "keyword line $it" }
        val text = lines.joinToString("\n")
        // All lines match "keyword", their windows will overlap and merge into one section
        val result = PdfCache.search(text, "keyword", contextWindow = 3)
        // Should only have one section header (one merged block)
        val sectionCount = result.split("--- Lines").size - 1
        assertTrue(sectionCount == 1, "Expected 1 merged section, got $sectionCount")
    }

    @Test
    fun `maxSections cap triggers truncation note`() {
        // Create text with many matches spread far apart
        val lines = mutableListOf<String>()
        for (i in 0 until 100) {
            if (i % 20 == 0) lines.add("match target here") else lines.add("filler line $i")
        }
        val text = lines.joinToString("\n")
        val result = PdfCache.search(text, "match target", contextWindow = 1, maxSections = 2)
        assertContains(result, "Mostrando")
    }

    @Test
    fun `charBudget cap triggers truncation note`() {
        val lines = (1..200).map { "This is a somewhat long filler line number $it for testing budget" }
        // Insert match near start
        val withMatch = listOf("match found here") + lines
        val text = withMatch.joinToString("\n")
        val result = PdfCache.search(text, "match", contextWindow = 50, charBudget = 100)
        assertContains(result, "Mostrando")
    }

    @Test
    fun `OR match capped at maxSections divided by 2`() {
        // 20 lines each with a different individual term, query has 2 terms
        // OR cap = maxSections/2 = 5 → at most 5 sections
        val lines = (1..40).map { if (it % 2 == 0) "alpha line $it" else "beta line $it" }
        val text = lines.joinToString("\n")
        val result = PdfCache.search(text, "alpha beta", contextWindow = 0, maxSections = 10)
        // In OR mode with maxSections/2 = 5, should not return all 20 matches
        val sectionCount = result.split("--- Lines").size - 1
        assertTrue(sectionCount <= 5, "Expected at most 5 OR sections, got $sectionCount")
    }
}
