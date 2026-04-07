package com.arkhamdb.mcp.tools

import com.arkhamdb.mcp.ArkhamDbClient
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("CardTools")

fun registerCardTools(server: Server, client: ArkhamDbClient) {
    // Tool: search_cards
    server.addTool(
        name = "search_cards",
        description = """Search for Arkham Horror LCG cards with optional filters. Returns a list of cards matching the criteria.
IMPORTANT: Card data is in Spanish. Use Spanish terms for searches:
- Traits: "Hechizo" (not "Spell"), "Aliado" (not "Ally"), "Arma" (not "Weapon"), "Objeto" (not "Item")
- Types: use type codes in English (asset, event, skill, investigator, treachery, enemy, agenda, act, location)
- Faction codes are in English (guardian, seeker, rogue, mystic, survivor, neutral)
- Card names and text are in Spanish""",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("name", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Búsqueda por nombre de carta (case-insensitive, en español)"))
                })
                put("faction", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Filter by faction code (guardian, seeker, rogue, mystic, survivor, neutral)"))
                })
                put("type", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Filter by card type code (asset, event, skill, investigator, treachery, enemy, agenda, act, location)"))
                })
                put("traits", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Filtrar por rasgo en español (e.g., 'Hechizo', 'Aliado', 'Arma') - coincide si algún rasgo contiene este texto"))
                })
                put("pack_code", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Filter by pack code (e.g., 'core', 'dwl', 'ptc')"))
                })
                put("xp", buildJsonObject {
                    put("type", JsonPrimitive("integer"))
                    put("description", JsonPrimitive("Filtrar por nivel de experiencia (0-5). Ej: 4 para la versión de 4 XP"))
                })
                put("include_encounter", buildJsonObject {
                    put("type", JsonPrimitive("boolean"))
                    put("description", JsonPrimitive("Include encounter cards (traiciones, enemigos, lugares de escenario). Default: false"))
                })
            }
        )
    ) { request ->
        val arguments = request.params.arguments
        val nameFilter = arguments.string("name")
        val factionFilter = arguments.string("faction")
        val typeFilter = arguments.string("type")
        val traitsFilter = arguments.string("traits")
        val packFilter = arguments.string("pack_code")
        val xpFilter = arguments.int("xp")
        val includeEncounter = arguments.boolean("include_encounter")

        logger.info("Searching cards - name: $nameFilter, faction: $factionFilter, type: $typeFilter, traits: $traitsFilter, pack: $packFilter, xp: $xpFilter, encounter: $includeEncounter")

        val cardsResult = if (includeEncounter) client.getAllCardsWithEncounter() else client.getAllCards()

        cardsResult
            .map { cards ->
                var filtered = cards

                nameFilter?.let { name ->
                    filtered = filtered.filter { card ->
                        card.name.contains(name, ignoreCase = true) ||
                                card.real_name?.contains(name, ignoreCase = true) == true
                    }
                }

                factionFilter?.let { faction ->
                    filtered = filtered.filter { card ->
                        card.faction_code.equals(faction, ignoreCase = true)
                    }
                }

                typeFilter?.let { type ->
                    filtered = filtered.filter { card ->
                        card.type_code.equals(type, ignoreCase = true)
                    }
                }

                traitsFilter?.let { traits ->
                    filtered = filtered.filter { card ->
                        card.traits?.contains(traits, ignoreCase = true) == true
                    }
                }

                packFilter?.let { pack ->
                    filtered = filtered.filter { card ->
                        card.pack_code.equals(pack, ignoreCase = true)
                    }
                }

                xpFilter?.let { xp ->
                    filtered = filtered.filter { card -> card.xp == xp }
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

    // Tool: get_card
    server.addTool(
        name = "get_card",
        description = "Get detailed information about a specific Arkham Horror LCG card by its code (e.g., '01001' for Roland Banks). Card data is in Spanish.",
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
        val code = request.params.arguments.string("code")

        if (code == null) {
            return@addTool CallToolResult(
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

    // Tool: get_cards_by_pack
    server.addTool(
        name = "get_cards_by_pack",
        description = """Get all cards from a specific pack/expansion, including encounter cards (enemies, treacheries, locations).
Use this tool to:
- Browse all cards in a campaign or pack (e.g., 'core' for Caja Básica)
- Find scenario encounter cards (enemies, traiciones, locations) not available through regular search
- Review campaign content for deck-building or campaign planning
Card data is in Spanish.""",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("pack_code", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Pack code (e.g., 'core', 'dwl', 'ptc', 'tfa', 'tcu', 'tde', 'tic', 'eoe', 'tsk')"))
                })
                put("type_code", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Optional: filter by card type (asset, event, skill, investigator, treachery, enemy, agenda, act, location)"))
                })
            },
            required = listOf("pack_code")
        )
    ) { request ->
        val arguments = request.params.arguments
        val packCode = arguments.string("pack_code")
        val typeCode = arguments.string("type_code")

        if (packCode == null) {
            return@addTool CallToolResult(
                content = listOf(TextContent(text = "Error: 'pack_code' parameter is required")),
                isError = true
            )
        }

        logger.info("Getting cards for pack: $packCode, type: $typeCode")

        client.getAllCardsWithEncounter()
            .map { cards ->
                var filtered = cards.filter { card ->
                    card.pack_code.equals(packCode, ignoreCase = true)
                }

                typeCode?.let { type ->
                    filtered = filtered.filter { card ->
                        card.type_code.equals(type, ignoreCase = true)
                    }
                }

                filtered = filtered.sortedBy { it.position ?: Int.MAX_VALUE }

                logger.info("Found ${filtered.size} cards for pack $packCode")

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
                logger.error("Error getting cards for pack $packCode", error)
                CallToolResult(
                    content = listOf(TextContent(text = "Error getting cards for pack $packCode: ${error.message}")),
                    isError = true
                )
            }
    }
}
