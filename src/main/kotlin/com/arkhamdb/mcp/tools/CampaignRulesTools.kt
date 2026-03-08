package com.arkhamdb.mcp.tools

import com.arkhamdb.mcp.ArkhamDbClient
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("CampaignRulesTools")

private data class CampaignInfo(
    val code: String,
    val name: String,
    val aliases: List<String>,
    val pdfFileName: String,
    val packCodes: List<String>
)

private val CAMPAIGNS = listOf(
    CampaignInfo(
        code = "core",
        name = "Night of the Zealot",
        aliases = listOf("night of the zealot", "zealot", "noz"),
        pdfFileName = "campaign_core_guide.pdf",
        packCodes = listOf("core")
    ),
    CampaignInfo(
        code = "dwl",
        name = "The Dunwich Legacy",
        aliases = listOf("dunwich", "dunwich legacy"),
        pdfFileName = "campaign_dwl_guide.pdf",
        packCodes = listOf("dwl", "tmm", "tece", "bota", "uau", "wog", "litas")
    ),
    CampaignInfo(
        code = "ptc",
        name = "The Path to Carcosa",
        aliases = listOf("carcosa", "path to carcosa"),
        pdfFileName = "campaign_ptc_guide.pdf",
        packCodes = listOf("ptc", "eotp", "lol", "uad", "wgd", "bsr", "dc")
    ),
    CampaignInfo(
        code = "tfa",
        name = "The Forgotten Age",
        aliases = listOf("forgotten age"),
        pdfFileName = "campaign_tfa_guide.pdf",
        packCodes = listOf("tfa", "tof", "tdoy", "sha", "tbb", "hoth")
    ),
    CampaignInfo(
        code = "tcu",
        name = "The Circle Undone",
        aliases = listOf("circle undone"),
        pdfFileName = "campaign_tcu_guide.pdf",
        packCodes = listOf("tcu", "tsn", "wos", "fgg", "uad", "icc", "bbt")
    ),
    CampaignInfo(
        code = "tde",
        name = "The Dream-Eaters",
        aliases = listOf("dream-eaters", "dream eaters"),
        pdfFileName = "campaign_tde_guide.pdf",
        packCodes = listOf("tde", "sfk", "tsh", "dsm", "pnr")
    ),
    CampaignInfo(
        code = "tic",
        name = "The Innsmouth Conspiracy",
        aliases = listOf("innsmouth", "innsmouth conspiracy"),
        pdfFileName = "campaign_tic_guide.pdf",
        packCodes = listOf("tic", "itd", "def", "hhg", "lif", "itm", "gar")
    ),
    CampaignInfo(
        code = "eoe",
        name = "Edge of the Earth",
        aliases = listOf("edge of the earth"),
        pdfFileName = "campaign_eoe_guide.pdf",
        packCodes = listOf("eoe", "atc", "ws", "fof", "poh", "ic")
    ),
    CampaignInfo(
        code = "tsk",
        name = "The Scarlet Keys",
        aliases = listOf("scarlet keys"),
        pdfFileName = "campaign_tsk_guide.pdf",
        packCodes = listOf("tsk")
    ),
    CampaignInfo(
        code = "fhv",
        name = "The Feast of Hemlock Vale",
        aliases = listOf("hemlock vale", "feast of hemlock vale", "hemlock"),
        pdfFileName = "campaign_fhv_guide.pdf",
        packCodes = listOf("fhv")
    ),
    CampaignInfo(
        code = "tdc",
        name = "The Drowned City",
        aliases = listOf("drowned city", "ciudad sumergida", "la ciudad sumergida"),
        pdfFileName = "campaign_tdc_guide.pdf",
        packCodes = listOf("tdcp", "tdcc")
    )
)

private fun resolveCampaign(input: String): CampaignInfo? {
    val lower = input.lowercase().trim()
    // 1. Exact code match
    CAMPAIGNS.find { it.code == lower }?.let { return it }
    // 2. Alias match
    CAMPAIGNS.find { campaign -> campaign.aliases.any { it.equals(lower, ignoreCase = true) } }?.let { return it }
    // 3. Name substring
    CAMPAIGNS.find { it.name.lowercase().contains(lower) }?.let { return it }
    return null
}

private fun availableCampaignsList(): String =
    CAMPAIGNS.joinToString("\n") { "- **${it.code}**: ${it.name}" }

fun registerCampaignRulesTools(server: Server, client: ArkhamDbClient) {
    server.addTool(
        name = "get_campaign_rules",
        description = """Get campaign-specific rules and encounter card information for Arkham Horror LCG campaigns.
Use this tool when the user asks about:
- Campaign-specific rules, setup, or special mechanics
- Cross-referencing campaign rules with the base game rules
- Listing encounter cards (enemies, treacheries, locations) for a campaign
Supported campaigns: core (Night of the Zealot), dwl (Dunwich Legacy), ptc (Path to Carcosa),
tfa (Forgotten Age), tcu (Circle Undone), tde (Dream-Eaters), tic (Innsmouth Conspiracy),
eoe (Edge of the Earth), tsk (Scarlet Keys), fhv (Feast of Hemlock Vale), tdc (The Drowned City).
IMPORTANT: Use Spanish terms for the 'query' parameter when searching PDF content.""",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("campaign", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Campaign pack code or name (e.g., 'dwl', 'dunwich', 'Dunwich Legacy', 'core')"))
                })
                put("query", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Optional Spanish search term for targeted lookup within the campaign guide and base rules"))
                })
                put("include_encounter_cards", buildJsonObject {
                    put("type", JsonPrimitive("boolean"))
                    put("description", JsonPrimitive("Fetch and list encounter cards (enemies, treacheries, locations, etc.) from all campaign packs. Default: false"))
                })
            },
            required = listOf("campaign")
        )
    ) { request ->
        val arguments = request.params.arguments
        val campaignInput = arguments.string("campaign")
        val query = arguments.string("query")
        val includeEncounterCards = arguments.boolean("include_encounter_cards")

        if (campaignInput == null) {
            return@addTool CallToolResult(
                content = listOf(TextContent(text = "Error: 'campaign' parameter is required.\n\nAvailable campaigns:\n${availableCampaignsList()}")),
                isError = true
            )
        }

        logger.info("get_campaign_rules — campaign: $campaignInput, query: $query, encounter: $includeEncounterCards")

        val campaignInfo = resolveCampaign(campaignInput) ?: return@addTool CallToolResult(
            content = listOf(
                TextContent(
                    text = "Unknown campaign: '$campaignInput'.\n\nAvailable campaigns:\n${availableCampaignsList()}"
                )
            ),
            isError = true
        )

        val result = StringBuilder()
        result.appendLine("# ${campaignInfo.name} — Campaign Rules")
        result.appendLine("**Code:** `${campaignInfo.code}` | **Packs:** ${campaignInfo.packCodes.joinToString(", ")}")
        result.appendLine()

        // Load campaign PDF
        val campaignPdfText = PdfCache.loadOrNull(campaignInfo.pdfFileName)
        if (campaignPdfText == null) {
            result.appendLine(
                "_Campaign guide PDF not found. Expected: `${campaignInfo.pdfFileName}` in `src/main/resources/pdfs/`_"
            )
            result.appendLine()
        }

        if (query != null) {
            // Source chain: campaign PDF first; fall back to base rules PDF if insufficient
            val rulings = SourceChain.primaryOrContrast(
                primary = SourceChain.Source("Campaign PDF") {
                    if (campaignPdfText == null) {
                        null
                    } else {
                        val searchResult = PdfCache.search(campaignPdfText, query)
                        if (searchResult.isBlank() || searchResult.startsWith("No results")) {
                            null
                        } else {
                            buildString {
                                appendLine("## Campaign Guide — Search: \"$query\"")
                                appendLine()
                                appendLine(searchResult)
                            }
                        }
                    }
                },
                fallbacks = listOf(
                    SourceChain.Source("Base Rules PDF") {
                        val baseRulesText = PdfCache.loadOrNull("arkham_horror_rules.pdf")
                        if (baseRulesText == null) {
                            null
                        } else {
                            val searchResult = PdfCache.search(baseRulesText, query)
                            if (searchResult.startsWith("No results")) {
                                null
                            } else {
                                buildString {
                                    appendLine("## Base Rules Cross-Reference — \"$query\"")
                                    appendLine()
                                    appendLine(searchResult)
                                }
                            }
                        }
                    }
                )
            )
            if (rulings != null) {
                result.appendLine(rulings)
                result.appendLine()
            }
        } else if (campaignPdfText != null) {
            // No query — return the first 100 lines (table of contents / index area)
            val headerLines = campaignPdfText.lines().take(100)
            result.appendLine("## Campaign Guide (índice)")
            result.appendLine()
            result.appendLine(headerLines.joinToString("\n"))
            result.appendLine()
            result.appendLine("_Mostrando las primeras 100 líneas. Para buscar contenido específico usa el parámetro `query`._")
        }

        // Encounter cards
        if (includeEncounterCards) {
            result.appendLine("## Encounter Cards")
            result.appendLine()
            client.getAllCardsWithEncounter()
                .onSuccess { cards ->
                    val campaignCards = cards.filter { card ->
                        campaignInfo.packCodes.any { it.equals(card.pack_code, ignoreCase = true) }
                    }

                    if (campaignCards.isEmpty()) {
                        result.appendLine("_No encounter cards found for packs: ${campaignInfo.packCodes.joinToString(", ")}_")
                    } else {
                        val grouped = campaignCards.groupBy { it.type_code }
                        val typeOrder = listOf("enemy", "treachery", "location", "agenda", "act")

                        for (typeCode in typeOrder) {
                            val typeCards = grouped[typeCode] ?: continue
                            val typeName = typeCards.first().type_name
                            result.appendLine("### $typeName (${typeCards.size})")
                            typeCards
                                .sortedWith(compareBy({ it.pack_code }, { it.position ?: Int.MAX_VALUE }))
                                .forEach { card ->
                                    val traits = if (!card.traits.isNullOrBlank()) " — ${card.traits}" else ""
                                    result.appendLine("- **${card.name}** [${card.pack_code}]$traits")
                                }
                            result.appendLine()
                        }

                        // Any remaining types
                        val otherTypes = grouped.keys.filter { it !in typeOrder }
                        for (typeCode in otherTypes) {
                            val typeCards = grouped[typeCode] ?: continue
                            val typeName = typeCards.first().type_name
                            result.appendLine("### $typeName (${typeCards.size})")
                            typeCards
                                .sortedWith(compareBy({ it.pack_code }, { it.position ?: Int.MAX_VALUE }))
                                .forEach { card ->
                                    result.appendLine("- **${card.name}** [${card.pack_code}]")
                                }
                            result.appendLine()
                        }
                    }
                }
                .onFailure { error ->
                    logger.error("Error fetching encounter cards for campaign ${campaignInfo.code}", error)
                    result.appendLine("_Error fetching encounter cards: ${error.message}_")
                }
        }

        CallToolResult(content = listOf(TextContent(text = result.toString())))
    }
}
