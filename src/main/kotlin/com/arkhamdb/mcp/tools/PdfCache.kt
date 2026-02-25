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
     * Multi-term search: splits [query] by whitespace and first tries AND (all terms on same line).
     * If no AND results are found, falls back to OR (any term on the line).
     */
    fun search(text: String, query: String, contextWindow: Int = 5): String {
        val lines = text.lines()
        val terms = query.lowercase().split("\\s+".toRegex()).filter { it.isNotBlank() }
        val matchRanges = mutableListOf<IntRange>()

        // AND pass: line must contain every term
        lines.forEachIndexed { index, line ->
            val lineLower = line.lowercase()
            if (terms.all { lineLower.contains(it) }) {
                val start = maxOf(0, index - contextWindow)
                val end = minOf(lines.size - 1, index + contextWindow)
                matchRanges.add(start..end)
            }
        }

        // OR fallback: line must contain at least one term
        if (matchRanges.isEmpty()) {
            lines.forEachIndexed { index, line ->
                val lineLower = line.lowercase()
                if (terms.any { lineLower.contains(it) }) {
                    val start = maxOf(0, index - contextWindow)
                    val end = minOf(lines.size - 1, index + contextWindow)
                    matchRanges.add(start..end)
                }
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

        val resultLines = mutableListOf<String>()
        merged.forEach { range ->
            resultLines.add("--- Lines ${range.first + 1}–${range.last + 1} ---")
            resultLines.addAll(lines.subList(range.first, range.last + 1))
            resultLines.add("")
        }

        return resultLines.joinToString("\n")
    }
}
