package com.arkhamdb.mcp.tools

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

object SourceChain {

    data class Source(
        val label: String,
        val fetch: suspend () -> String?
    )

    /**
     * Primary wins if sufficient (>= minChars).
     * If primary is insufficient, all fallbacks run in parallel and results are merged with primary's partial output.
     * Returns null only if all sources return empty/null.
     */
    suspend fun primaryOrContrast(
        primary: Source,
        fallbacks: List<Source>,
        minChars: Int = 150,
        separator: String = "\n\n---\n\n"
    ): String? {
        val primaryResult = primary.fetch()
        if (!primaryResult.isNullOrBlank() && primaryResult.length >= minChars) {
            return primaryResult
        }

        // Primary insufficient — run all fallbacks in parallel
        val fallbackResults = coroutineScope {
            fallbacks.map { source -> async { source.fetch() } }.map { it.await() }
        }

        val parts = mutableListOf<String>()
        if (!primaryResult.isNullOrBlank()) parts.add(primaryResult)
        fallbackResults.forEach { fallbackResult ->
            if (!fallbackResult.isNullOrBlank()) parts.add(fallbackResult)
        }

        return if (parts.isEmpty()) null else parts.joinToString(separator)
    }

    /**
     * All sources run in parallel. Results are concatenated.
     * Returns null only if all sources return empty/null.
     */
    suspend fun mergeAll(
        sources: List<Source>,
        separator: String = "\n\n---\n\n"
    ): String? = coroutineScope {
        val results = sources.map { source -> async { source.fetch() } }.map { it.await() }
        val parts = results.filterNotNull().filter { it.isNotBlank() }
        if (parts.isEmpty()) null else parts.joinToString(separator)
    }
}
