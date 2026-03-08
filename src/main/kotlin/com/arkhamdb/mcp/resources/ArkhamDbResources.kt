package com.arkhamdb.mcp.resources

import com.arkhamdb.mcp.ArkhamDbClient
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.ReadResourceResult
import io.modelcontextprotocol.kotlin.sdk.types.TextResourceContents
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("ArkhamDbResources")

fun registerResources(server: Server, client: ArkhamDbClient) {
    // Resource: arkhamdb://cards - Complete card database
    server.addResource(
        uri = "arkhamdb://cards",
        name = "All Cards",
        description = "Complete card database for Arkham Horror LCG",
        mimeType = "application/json"
    ) { request ->
        logger.info("Reading all cards resource")

        client.getAllCards()
            .map { cards ->
                ReadResourceResult(
                    contents = listOf(
                        TextResourceContents(
                            text = Json.encodeToString(
                                kotlinx.serialization.builtins.ListSerializer(
                                    com.arkhamdb.mcp.models.Card.serializer()
                                ),
                                cards
                            ),
                            uri = request.uri,
                            mimeType = "application/json"
                        )
                    )
                )
            }
            .getOrElse { error ->
                logger.error("Error reading cards resource", error)
                ReadResourceResult(
                    contents = listOf(
                        TextResourceContents(
                            text = "Error reading cards: ${error.message}",
                            uri = request.uri,
                            mimeType = "text/plain"
                        )
                    )
                )
            }
    }

    // Resource: arkhamdb://packs - All packs
    server.addResource(
        uri = "arkhamdb://packs",
        name = "All Packs",
        description = "List of all card packs and expansions",
        mimeType = "application/json"
    ) { request ->
        logger.info("Reading all packs resource")

        client.getAllPacks()
            .map { packs ->
                ReadResourceResult(
                    contents = listOf(
                        TextResourceContents(
                            text = Json.encodeToString(
                                kotlinx.serialization.builtins.ListSerializer(
                                    com.arkhamdb.mcp.models.Pack.serializer()
                                ),
                                packs
                            ),
                            uri = request.uri,
                            mimeType = "application/json"
                        )
                    )
                )
            }
            .getOrElse { error ->
                logger.error("Error reading packs resource", error)
                ReadResourceResult(
                    contents = listOf(
                        TextResourceContents(
                            text = "Error reading packs: ${error.message}",
                            uri = request.uri,
                            mimeType = "text/plain"
                        )
                    )
                )
            }
    }
}
