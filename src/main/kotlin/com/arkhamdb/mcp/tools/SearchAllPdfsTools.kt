package com.arkhamdb.mcp.tools

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("SearchAllPdfsTools")

fun registerSearchAllPdfsTools(server: Server) {
    server.addTool(
        name = "search_all_pdfs",
        description = """Search across all Arkham Horror LCG PDF sources simultaneously: Rules Reference and FAQ/Errata.
PREFER this tool over calling get_rules and get_faq separately when:
- Answering questions about card interactions that may involve both rules and official rulings/errata
- Looking up mechanics that could have been clarified or modified by the FAQ (e.g. limbo, timing, keywords)
- Any question where comprehensive rules coverage is needed in a single call
Returns results grouped by source (Reglamento / FAQ) with a controlled budget per source to avoid
filling the context window — each source is capped at ~5000 chars and 8 sections.
IMPORTANT: Use Spanish terms in the query (e.g., 'limbo', 'alterar el destino', 'prueba de habilidad').""",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("query", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Término de búsqueda en español (e.g., 'limbo', 'alterar el destino', 'evasión')"))
                })
            },
            required = listOf("query")
        )
    ) { request ->
        val query = request.params.arguments.string("query")

        if (query == null) {
            return@addTool CallToolResult(
                content = listOf(TextContent(text = "Error: el parámetro 'query' es obligatorio.")),
                isError = true
            )
        }

        logger.info("search_all_pdfs — query: $query")

        // Both PDFs are in-memory after first load — search them in parallel
        val (rulesResult, faqResult) = coroutineScope {
            val rules = async {
                PdfCache.loadOrNull("arkham_horror_rules.pdf")?.let {
                    PdfCache.search(it, query, contextWindow = 4, maxSections = 8, charBudget = 5_000)
                }
            }
            val faq = async {
                PdfCache.loadOrNull("arkham_horror_faq.pdf")?.let {
                    PdfCache.search(it, query, contextWindow = 4, maxSections = 8, charBudget = 5_000)
                }
            }
            Pair(rules.await(), faq.await())
        }

        if (rulesResult == null && faqResult == null) {
            return@addTool CallToolResult(
                content = listOf(TextContent(text = "No hay PDFs disponibles. Coloca los archivos en src/main/resources/pdfs/")),
                isError = true
            )
        }

        val result = StringBuilder()
        result.appendLine("# Búsqueda en todas las fuentes PDF — \"$query\"")
        result.appendLine()

        if (rulesResult != null) {
            result.appendLine("## Reglamento Oficial")
            result.appendLine()
            result.appendLine(rulesResult)
            result.appendLine()
        } else {
            result.appendLine("_Reglamento no disponible._")
            result.appendLine()
        }

        if (faqResult != null) {
            result.appendLine("## FAQ y Erratas")
            result.appendLine()
            result.appendLine(faqResult)
        } else {
            result.appendLine("_FAQ no disponible._")
        }

        CallToolResult(content = listOf(TextContent(text = result.toString())))
    }
}
