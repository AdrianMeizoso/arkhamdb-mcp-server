package com.arkhamdb.mcp.tools

import com.arkhamdb.mcp.ArkhamDbClient
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("PackTools")

fun registerPackTools(server: Server, client: ArkhamDbClient) {
    // Tool: list_packs
    server.addTool(
        name = "list_packs",
        description = "List all available card packs and expansions for Arkham Horror LCG.",
        inputSchema = ToolSchema(
            properties = buildJsonObject {}
        )
    ) { _ ->
        logger.info("Listing all packs")

        client.getAllPacks()
            .map { packs ->
                CallToolResult(
                    content = listOf(
                        TextContent(
                            text = Json.encodeToString(
                                kotlinx.serialization.builtins.ListSerializer(
                                    com.arkhamdb.mcp.models.Pack.serializer()
                                ),
                                packs
                            )
                        )
                    )
                )
            }
            .getOrElse { error ->
                logger.error("Error listing packs", error)
                CallToolResult(
                    content = listOf(TextContent(text = "Error listing packs: ${error.message}")),
                    isError = true
                )
            }
    }
}
