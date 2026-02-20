package com.arkhamdb.mcp.tools

import com.arkhamdb.mcp.ArkhamDbClient
import io.modelcontextprotocol.kotlin.sdk.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("DecklistTools")

fun registerDecklistTools(server: Server, client: ArkhamDbClient) {
    // Tool: get_decklist
    server.addTool(
        name = "get_decklist",
        description = "Retrieve a public decklist by its ID from ArkhamDB. Returns the decklist with card slots, investigator, and metadata.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("id", buildJsonObject {
                    put("type", JsonPrimitive("number"))
                    put("description", JsonPrimitive("The decklist ID number"))
                })
            },
            required = listOf("id")
        )
    ) { request ->
        runBlocking {
            val arguments = request.params.arguments ?: JsonObject(emptyMap())
            val id = arguments["id"]?.jsonPrimitive?.intOrNull

            if (id == null) {
                return@runBlocking CallToolResult(
                    content = listOf(TextContent(text = "Error: 'id' parameter is required and must be a number")),
                    isError = true
                )
            }

            logger.info("Getting decklist with id: $id")

            client.getDecklist(id)
                .map { decklist ->
                    CallToolResult(
                        content = listOf(
                            TextContent(
                                text = Json.encodeToString(com.arkhamdb.mcp.models.Decklist.serializer(), decklist)
                            )
                        )
                    )
                }
                .getOrElse { error ->
                    logger.error("Error getting decklist $id", error)
                    CallToolResult(
                        content = listOf(TextContent(text = "Decklist not found: $id. Error: ${error.message}")),
                        isError = true
                    )
                }
        }
    }
}
