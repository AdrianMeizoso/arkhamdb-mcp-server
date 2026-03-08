package com.arkhamdb.mcp.tools

import com.arkhamdb.mcp.ArkhamDbClient
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("CardFaqTools")

fun registerCardFaqTools(server: Server, client: ArkhamDbClient) {
    server.addTool(
        name = "get_card_faq",
        description = """Get FAQ rulings and reviews for a specific Arkham Horror LCG card.
Primary source: ArkhamDB API rulings (if sufficient, no PDF is queried).
Falls back to FAQ PDF and base rules PDF (in parallel) when API has no rulings.
Provide card_code (e.g., '01006') and/or card_name (in Spanish) to look up the card.
Use this when a player wants to know how a specific card works in edge cases.""",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("card_code", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Card code (e.g., '01006'). Used to fetch API FAQ. If provided without card_name, the card name is resolved automatically."))
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

            // Source chain: API FAQ first; fall back to FAQ PDF + base rules PDF (parallel) if insufficient
            val rulings = SourceChain.primaryOrContrast(
                primary = SourceChain.Source("API FAQ") {
                    if (cardCode == null) {
                        null
                    } else {
                        val sb = StringBuilder()
                        sb.appendLine("## Rulings oficiales (ArkhamDB)")
                        sb.appendLine()
                        client.getCardFaq(cardCode)
                            .onSuccess { faqs ->
                                if (faqs.isEmpty()) {
                                    sb.appendLine("_No hay FAQ oficial disponible para esta carta en ArkhamDB._")
                                } else {
                                    faqs.forEach { faq -> faq.text?.let { sb.appendLine(it) } }
                                }
                            }
                            .onFailure { error ->
                                logger.warn("Could not fetch FAQ for $cardCode: ${error.message}")
                                sb.appendLine("_No se pudo obtener el FAQ de la API: ${error.message}_")
                            }
                        sb.toString()
                    }
                },
                fallbacks = listOf(
                    SourceChain.Source("FAQ PDF") {
                        val searchName = resolvedCardName
                        val faqText = if (searchName != null) PdfCache.loadOrNull("arkham_horror_faq.pdf") else null
                        if (searchName == null || faqText == null) {
                            null
                        } else {
                            val searchResult = PdfCache.search(faqText, searchName, contextWindow = 8)
                            if (searchResult.startsWith("No results")) {
                                null
                            } else {
                                buildString {
                                    appendLine("## FAQ Oficial (PDF en Español)")
                                    appendLine()
                                    appendLine(searchResult)
                                }
                            }
                        }
                    },
                    SourceChain.Source("Base Rules PDF") {
                        val searchName = resolvedCardName
                        val rulesText = if (searchName != null) PdfCache.loadOrNull("arkham_horror_rules.pdf") else null
                        if (searchName == null || rulesText == null) {
                            null
                        } else {
                            val searchResult = PdfCache.search(rulesText, searchName, contextWindow = 8)
                            if (searchResult.startsWith("No results")) {
                                null
                            } else {
                                buildString {
                                    appendLine("## Reglamento (PDF en Español)")
                                    appendLine()
                                    appendLine(searchResult)
                                }
                            }
                        }
                    }
                )
            )

            if (rulings != null) {
                result.append(rulings)
            } else {
                result.appendLine("_No se encontraron rulings ni referencias en ninguna fuente._")
            }

            CallToolResult(content = listOf(TextContent(text = result.toString())))
        }
    }
}
