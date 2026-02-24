package com.arkhamdb.mcp.tools

import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.*
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.slf4j.LoggerFactory
import java.io.File

private val logger = LoggerFactory.getLogger("PdfTools")

private fun loadPdfText(fileName: String): String {
    val resourceStream = object {}::class.java.classLoader.getResourceAsStream("pdfs/$fileName")
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

private fun searchInText(text: String, query: String): String {
    val lines = text.lines()
    val queryLower = query.lowercase()
    val matchingLines = mutableListOf<String>()
    val contextWindow = 5

    lines.forEachIndexed { index, line ->
        if (line.lowercase().contains(queryLower)) {
            val start = maxOf(0, index - contextWindow)
            val end = minOf(lines.size - 1, index + contextWindow)
            matchingLines.add("--- Match at line ${index + 1} ---")
            matchingLines.addAll(lines.subList(start, end + 1))
            matchingLines.add("")
        }
    }

    return if (matchingLines.isEmpty()) {
        "No results found for: \"$query\""
    } else {
        matchingLines.joinToString("\n")
    }
}

fun registerPdfTools(server: Server) {
    // Tool: get_rules
    server.addTool(
        name = "get_rules",
        description = """Get the official rules for Arkham Horror: The Card Game from the Rules Reference PDF.
Use this tool when the user asks about:
- How to play, game rules, mechanics, or procedures
- Specific game concepts (actions, reactions, triggers, keywords, etc.)
- Setup or scenario instructions
- Card interactions or rule clarifications
Optionally provide a 'query' to search for a specific topic within the rules.
IMPORTANT: Las reglas están en español. Usar términos en español en la búsqueda (e.g., 'perdición', 'acción', 'evadir', 'prueba de habilidad', 'horror', 'daño').""",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("query", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Término o tema a buscar en las reglas (en español, e.g., 'evadir', 'acción', 'perdición', 'prueba de habilidad')"))
                })
            }
        )
    ) { request ->
        val arguments = request.params.arguments ?: JsonObject(emptyMap())
        val query = arguments["query"]?.jsonPrimitive?.contentOrNull

        logger.info("get_rules called, query: $query")

        try {
            val fullText = loadPdfText("arkham_horror_rules.pdf")
            val result = if (query != null) {
                logger.info("Searching rules for: $query")
                val searchResult = searchInText(fullText, query)
                "## Rules Reference — Search: \"$query\"\n\n$searchResult"
            } else {
                "## Arkham Horror LCG — Rules Reference\n\n$fullText"
            }

            CallToolResult(content = listOf(TextContent(text = result)))
        } catch (e: Exception) {
            logger.error("Error loading rules PDF", e)
            CallToolResult(
                content = listOf(TextContent(text = "Error loading rules: ${e.message}")),
                isError = true
            )
        }
    }

    // Tool: get_faq
    server.addTool(
        name = "get_faq",
        description = """Get the official FAQ and errata for Arkham Horror: The Card Game.
Use this tool when the user asks about:
- Official rulings, clarifications, or errata on specific cards
- FAQs, frequently asked questions, or controversial rules
- Whether a card has been updated or changed in an errata
- Specific card interactions that may be in the FAQ
Optionally provide a 'query' to search for a specific card name or topic.
IMPORTANT: El FAQ está en español. Usar términos en español en la búsqueda (e.g., 'perdición', 'prueba de habilidad', 'evadir', 'acción gratuita').""",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("query", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Nombre de carta o tema a buscar en el FAQ (en español, e.g., 'Barricada', 'prueba de habilidad', 'perdición')"))
                })
            }
        )
    ) { request ->
        val arguments = request.params.arguments ?: JsonObject(emptyMap())
        val query = arguments["query"]?.jsonPrimitive?.contentOrNull

        logger.info("get_faq called, query: $query")

        try {
            val fullText = loadPdfText("arkham_horror_faq.pdf")
            val result = if (query != null) {
                logger.info("Searching FAQ for: $query")
                val searchResult = searchInText(fullText, query)
                "## FAQ & Errata — Search: \"$query\"\n\n$searchResult"
            } else {
                "## Arkham Horror LCG — FAQ & Errata\n\n$fullText"
            }

            CallToolResult(content = listOf(TextContent(text = result)))
        } catch (e: Exception) {
            logger.error("Error loading FAQ PDF", e)
            CallToolResult(
                content = listOf(TextContent(text = "Error loading FAQ: ${e.message}")),
                isError = true
            )
        }
    }
}
