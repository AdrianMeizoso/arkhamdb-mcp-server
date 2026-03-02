package com.arkhamdb.mcp.tools

import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.slf4j.LoggerFactory
import java.io.File
import java.util.concurrent.ConcurrentHashMap

private val logger = LoggerFactory.getLogger("PdfCache")

/**
 * Thread-safe in-memory cache for PDF text extraction.
 * Cache key is the filename (e.g., "arkham_horror_rules.pdf"); "pdfs/" is prepended internally.
 */
object PdfCache {
    private val cache = ConcurrentHashMap<String, String>()

    /**
     * Load and cache the PDF text. Throws if the file is not found.
     */
    fun load(fileName: String): String {
        return cache.computeIfAbsent(fileName) { loadFromDisk(it) }
    }

    /**
     * Load and cache the PDF text. Returns null if the file is not found or cannot be read.
     */
    fun loadOrNull(fileName: String): String? {
        return try {
            cache.computeIfAbsent(fileName) { loadFromDisk(it) }
        } catch (e: Exception) {
            logger.warn("PDF not available: $fileName — ${e.message}")
            null
        }
    }

    private fun loadFromDisk(fileName: String): String {
        val resourceStream = PdfCache::class.java.classLoader.getResourceAsStream("pdfs/$fileName")
        return if (resourceStream != null) {
            logger.info("Loading PDF from resources: $fileName")
            resourceStream.use { stream ->
                val document = Loader.loadPDF(stream.readBytes())
                val stripper = PDFTextStripper()
                val text = stripper.getText(document)
                document.close()
                text
            }
        } else {
            val externalPath = File("src/main/resources/pdfs/$fileName")
            if (externalPath.exists()) {
                logger.info("Loading PDF from file: ${externalPath.absolutePath}")
                val document = Loader.loadPDF(externalPath)
                val stripper = PDFTextStripper()
                val text = stripper.getText(document)
                document.close()
                text
            } else {
                throw IllegalStateException("PDF not found: $fileName. Place it in src/main/resources/pdfs/")
            }
        }
    }

    /**
     * Search for [query] in [text], returning matching lines with [contextWindow] lines of context.
     * Overlapping context windows are merged to avoid duplicate output.
     *
     * Search strategy (in order of priority):
     * 1. Exact phrase match (multi-word queries searched as a literal string)
     * 2. AND match: all terms appear on the same line
     * 3. OR fallback: any term appears on the line (capped at [maxSections] / 2)
     *
     * Results are capped at [maxSections] sections and [charBudget] characters to avoid
     * overwhelming the LLM context. If truncated, a note is appended.
     */
    fun search(
        text: String,
        query: String,
        contextWindow: Int = 5,
        maxSections: Int = 20,
        charBudget: Int = 12_000
    ): String {
        val lines = text.lines()
        val queryLower = query.lowercase().trim()
        val terms = queryLower.split("\\s+".toRegex()).filter { it.isNotBlank() }
        val matchRanges = mutableListOf<IntRange>()

        fun addMatch(index: Int) {
            val start = maxOf(0, index - contextWindow)
            val end = minOf(lines.size - 1, index + contextWindow)
            matchRanges.add(start..end)
        }

        // Pass 1: exact phrase (only meaningful for multi-word queries)
        if (terms.size > 1) {
            lines.forEachIndexed { index, line ->
                if (line.lowercase().contains(queryLower)) addMatch(index)
            }
        }

        // Pass 2: AND — all terms on the same line
        if (matchRanges.isEmpty()) {
            lines.forEachIndexed { index, line ->
                val lineLower = line.lowercase()
                if (terms.all { lineLower.contains(it) }) addMatch(index)
            }
        }

        // Pass 3: OR fallback — any term on the line, capped early to avoid explosion
        if (matchRanges.isEmpty()) {
            val orCap = maxSections / 2
            lines.forEachIndexed { index, line ->
                if (matchRanges.size >= orCap) return@forEachIndexed
                val lineLower = line.lowercase()
                if (terms.any { lineLower.contains(it) }) addMatch(index)
            }
        }

        if (matchRanges.isEmpty()) return "No results found for: \"$query\""

        // Merge overlapping or adjacent ranges
        val sorted = matchRanges.sortedBy { it.first }
        val merged = mutableListOf(sorted[0])
        for (i in 1 until sorted.size) {
            val last = merged.last()
            val current = sorted[i]
            if (current.first <= last.last + 1) {
                merged[merged.size - 1] = last.first..maxOf(last.last, current.last)
            } else {
                merged.add(current)
            }
        }

        // Cap sections
        val truncatedSections = merged.size > maxSections
        val sectionsToRender = if (truncatedSections) merged.take(maxSections) else merged

        val resultLines = mutableListOf<String>()
        var charCount = 0
        var truncatedChars = false

        for (range in sectionsToRender) {
            val header = "--- Lines ${range.first + 1}–${range.last + 1} ---"
            val block = lines.subList(range.first, range.last + 1).joinToString("\n")
            val chunk = "$header\n$block\n\n"
            if (charCount + chunk.length > charBudget) {
                truncatedChars = true
                break
            }
            resultLines.add(chunk)
            charCount += chunk.length
        }

        if (truncatedSections || truncatedChars) {
            val shown = resultLines.size
            val total = merged.size
            resultLines.add(
                "_Mostrando $shown de $total secciones. Refina la búsqueda con términos más específicos para ver más._"
            )
        }

        return resultLines.joinToString("")
    }
}
