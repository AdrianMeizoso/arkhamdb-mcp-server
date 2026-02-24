package com.arkhamdb.mcp.tools

import com.arkhamdb.mcp.ArkhamDbClient
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.slf4j.LoggerFactory
import java.io.File

private val logger = LoggerFactory.getLogger("CardFaqTools")

private fun loadFaqPdfText(): String? {
    val resourceStream = object {}::class.java.classLoader.getResourceAsStream("pdfs/arkham_horror_faq.pdf")
    return if (resourceStream != null) {
        resourceStream.use { stream ->
            val document = Loader.loadPDF(stream.readBytes())
            val stripper = PDFTextStripper()
            val text = stripper.getText(document)
            document.close()
            text
        }
    } else {
        val externalPath = File("src/main/resources/pdfs/arkham_horror_faq.pdf")
        if (externalPath.exists()) {
            val document = Loader.loadPDF(externalPath)
            val stripper = PDFTextStripper()
            val text = stripper.getText(document)
            document.close()
            text
        } else {
            null
        }
    }
}

private fun searchFaqForCard(faqText: String, cardName: String): String {
    val lines = faqText.lines()
    val queryLower = cardName.lowercase()
    val matchingLines = mutableListOf<String>()
    val contextWindow = 8

    lines.forEachIndexed { index, line ->
        if (line.lowercase().contains(queryLower)) {
            val start = maxOf(0, index - contextWindow)
            val end = minOf(lines.size - 1, index + contextWindow)
            matchingLines.add("--- Coincidencia en línea ${index + 1} ---")
            matchingLines.addAll(lines.subList(start, end + 1))
            matchingLines.add("")
        }
    }

    return if (matchingLines.isEmpty()) {
        "No se encontraron resultados en el FAQ para: \"$cardName\""
    } else {
        matchingLines.joinToString("\n")
    }
}

fun registerCardFaqTools(server: Server, client: ArkhamDbClient) {
    server.addTool(
        name = "get_card_faq",
        description = """Get FAQ rulings and reviews for a specific Arkham Horror LCG card.
Combines two sources:
1. Official FAQ PDF (errata and official rulings in Spanish)
2. ArkhamDB community reviews/rulings from the public API
Provide card_code (e.g., '01006') and/or card_name (in Spanish) to look up the card.
Use this when a player wants to know how a specific card works in edge cases.""",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("card_code", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Card code (e.g., '01006'). Used to fetch API reviews. If provided without card_name, the card name is resolved automatically."))
                })
                put("card_name", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Card name in Spanish (e.g., 'Revólver .38 de Roland'). Used for PDF FAQ search."))
                })
            }
        )
    ) { request ->
        runBlocking {
            val arguments = request.params.arguments ?: JsonObject(emptyMap())
            val cardCode = arguments["card_code"]?.jsonPrimitive?.contentOrNull
            val cardNameParam = arguments["card_name"]?.jsonPrimitive?.contentOrNull

            if (cardCode == null && cardNameParam == null) {
                return@runBlocking CallToolResult(
                    content = listOf(TextContent(text = "Error: se requiere al menos 'card_code' o 'card_name'")),
                    isError = true
                )
            }

            val result = StringBuilder()
            result.appendLine("# FAQ de Carta — Arkham Horror LCG")
            result.appendLine()

            // Resolve card name from code if needed
            var resolvedCardName = cardNameParam
            if (cardCode != null && resolvedCardName == null) {
                client.getCard(cardCode).onSuccess { card ->
                    resolvedCardName = card.name
                    result.appendLine("**Carta:** ${card.name} (${card.code})")
                    result.appendLine("**Tipo:** ${card.type_name} | **Facción:** ${card.faction_name}")
                    result.appendLine()
                }.onFailure {
                    logger.warn("Could not resolve card name for code $cardCode")
                }
            } else if (cardCode != null && resolvedCardName != null) {
                result.appendLine("**Carta:** $resolvedCardName ($cardCode)")
                result.appendLine()
            } else {
                result.appendLine("**Carta:** $resolvedCardName")
                result.appendLine()
            }

            // Step 1: Fetch API reviews if card_code provided
            if (cardCode != null) {
                result.appendLine("## Rulings y Reviews (ArkhamDB API)")
                result.appendLine()
                client.getCardReviews(cardCode)
                    .onSuccess { reviews ->
                        if (reviews.isEmpty()) {
                            result.appendLine("_No hay reviews disponibles para esta carta en ArkhamDB._")
                        } else {
                            reviews.forEachIndexed { i, review ->
                                result.appendLine("### Review ${i + 1}${if (review.title != null) ": ${review.title}" else ""}")
                                review.author_username?.let { result.appendLine("**Autor:** $it") }
                                review.date_creation?.let { result.appendLine("**Fecha:** $it") }
                                result.appendLine()
                                result.appendLine(review.body)
                                result.appendLine()
                            }
                        }
                    }
                    .onFailure { error ->
                        logger.warn("Could not fetch reviews for $cardCode: ${error.message}")
                        result.appendLine("_No se pudieron obtener reviews de la API: ${error.message}_")
                    }
            }

            // Step 2: Search FAQ PDF for card name
            val searchName = resolvedCardName
            if (searchName != null) {
                result.appendLine()
                result.appendLine("## FAQ Oficial (PDF en Español)")
                result.appendLine()
                try {
                    val faqText = loadFaqPdfText()
                    if (faqText != null) {
                        val faqResult = searchFaqForCard(faqText, searchName)
                        result.appendLine(faqResult)
                    } else {
                        result.appendLine("_El PDF del FAQ no está disponible. Coloca 'arkham_horror_faq.pdf' en src/main/resources/pdfs/_")
                    }
                } catch (e: Exception) {
                    logger.error("Error loading FAQ PDF for card FAQ tool", e)
                    result.appendLine("_Error al cargar el PDF del FAQ: ${e.message}_")
                }
            }

            CallToolResult(content = listOf(TextContent(text = result.toString())))
        }
    }
}
