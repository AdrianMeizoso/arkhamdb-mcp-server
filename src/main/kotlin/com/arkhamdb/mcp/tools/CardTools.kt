package com.arkhamdb.mcp.tools

import com.arkhamdb.mcp.ArkhamDbClient
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("CardTools")

fun registerCardTools(server: Server, client: ArkhamDbClient) {
    // Tool: search_cards
    server.addTool(
        name = "search_cards",
        description = "Search for Arkham Horror LCG cards with optional filters. Returns a list of cards matching the criteria.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("name", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Card name substring search (case-insensitive)"))
                })
                put("faction", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Filter by faction code (guardian, seeker, rogue, mystic, survivor, neutral)"))
                })
                put("type", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Filter by card type (asset, event, skill, investigator, treachery, enemy, etc.)"))
                })
                put("traits", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Filter by card traits (weapon, spell, ally, item, etc.) - matches if any trait contains this text"))
                })
            }
        )
    ) { request ->
        runBlocking {
            val arguments = request.params.arguments ?: JsonObject(emptyMap())
            val nameFilter = arguments["name"]?.jsonPrimitive?.contentOrNull
            val factionFilter = arguments["faction"]?.jsonPrimitive?.contentOrNull
            val typeFilter = arguments["type"]?.jsonPrimitive?.contentOrNull
            val traitsFilter = arguments["traits"]?.jsonPrimitive?.contentOrNull

            logger.info("Searching cards with filters - name: $nameFilter, faction: $factionFilter, type: $typeFilter, traits: $traitsFilter")

            client.getAllCards()
                .map { cards ->
                    var filtered = cards

                    // Apply name filter
                    nameFilter?.let { name ->
                        filtered = filtered.filter { card ->
                            card.name.contains(name, ignoreCase = true) ||
                                    card.real_name?.contains(name, ignoreCase = true) == true
                        }
                    }

                    // Apply faction filter
                    factionFilter?.let { faction ->
                        filtered = filtered.filter { card ->
                            card.faction_code.equals(faction, ignoreCase = true)
                        }
                    }

                    // Apply type filter
                    typeFilter?.let { type ->
                        filtered = filtered.filter { card ->
                            card.type_code.equals(type, ignoreCase = true)
                        }
                    }

                    // Apply traits filter
                    traitsFilter?.let { traits ->
                        filtered = filtered.filter { card ->
                            card.traits?.contains(traits, ignoreCase = true) == true
                        }
                    }

                    logger.info("Found ${filtered.size} cards matching filters")

                    CallToolResult(
                        content = listOf(
                            TextContent(
                                text = Json.encodeToString(
                                    kotlinx.serialization.builtins.ListSerializer(
                                        com.arkhamdb.mcp.models.Card.serializer()
                                    ),
                                    filtered
                                )
                            )
                        )
                    )
                }
                .getOrElse { error ->
                    logger.error("Error searching cards", error)
                    CallToolResult(
                        content = listOf(TextContent(text = "Error searching cards: ${error.message}")),
                        isError = true
                    )
                }
        }
    }

    // Tool: get_card
    server.addTool(
        name = "get_card",
        description = "Get detailed information about a specific Arkham Horror LCG card by its code (e.g., '01001' for Roland Banks).",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("code", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("The card code (e.g., '01001')"))
                })
            },
            required = listOf("code")
        )
    ) { request ->
        runBlocking {
            val arguments = request.params.arguments ?: JsonObject(emptyMap())
            val code = arguments["code"]?.jsonPrimitive?.contentOrNull

            if (code == null) {
                return@runBlocking CallToolResult(
                    content = listOf(TextContent(text = "Error: 'code' parameter is required")),
                    isError = true
                )
            }

            logger.info("Getting card with code: $code")

            client.getCard(code)
                .map { card ->
                    CallToolResult(
                        content = listOf(
                            TextContent(
                                text = Json.encodeToString(com.arkhamdb.mcp.models.Card.serializer(), card)
                            )
                        )
                    )
                }
                .getOrElse { error ->
                    logger.error("Error getting card $code", error)
                    CallToolResult(
                        content = listOf(TextContent(text = "Card not found: $code. Error: ${error.message}")),
                        isError = true
                    )
                }
        }
    }
}
