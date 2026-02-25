package com.arkhamdb.mcp

import com.arkhamdb.mcp.resources.registerResources
import com.arkhamdb.mcp.resources.registerPdfResources
import com.arkhamdb.mcp.tools.registerCampaignRulesTools
import com.arkhamdb.mcp.tools.registerCardFaqTools
import com.arkhamdb.mcp.tools.registerCardTools
import com.arkhamdb.mcp.tools.registerDecklistTools
import com.arkhamdb.mcp.tools.registerPackTools
import com.arkhamdb.mcp.tools.registerPdfTools
import io.modelcontextprotocol.kotlin.sdk.Implementation
import io.modelcontextprotocol.kotlin.sdk.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlinx.io.asSource
import kotlinx.io.asSink
import kotlinx.io.buffered
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("Main")

fun main(): Unit = runBlocking {
    try {
        logger.info("Starting ArkhamDB MCP Server")

        // 1. Initialize Ktor client with caching
        val arkhamClient = ArkhamDbClient()

        // 2. Create MCP server with capabilities
        val server = Server(
            serverInfo = Implementation(
                name = "arkhamdb-mcp-server",
                version = "1.0.0"
            ),
            options = ServerOptions(
                capabilities = ServerCapabilities(
                    tools = ServerCapabilities.Tools(listChanged = false),
                    resources = ServerCapabilities.Resources(
                        subscribe = false,
                        listChanged = false
                    )
                )
            )
        )

        // 3. Register all tools
        logger.info("Registering tools...")
        registerCardTools(server, arkhamClient)
        registerPackTools(server, arkhamClient)
        registerDecklistTools(server, arkhamClient)
        registerPdfTools(server)
        registerCardFaqTools(server, arkhamClient)
        registerCampaignRulesTools(server, arkhamClient)

        // 4. Register all resources
        logger.info("Registering resources...")
        registerResources(server, arkhamClient)
        registerPdfResources(server)

        // 5. Setup STDIO transport
        logger.info("Setting up STDIO transport...")
        val transport = StdioServerTransport(
            inputStream = System.`in`.asSource().buffered(),
            outputStream = System.out.asSink().buffered()
        )

        // 6. Create session and run
        logger.info("Creating server session...")
        val session = server.createSession(transport)

        logger.info("ArkhamDB MCP Server is running and ready to accept requests")

        // Keep the session running indefinitely
        awaitCancellation()
    } catch (e: Exception) {
        logger.error("Fatal error in ArkhamDB MCP Server", e)
        throw e
    }
}
