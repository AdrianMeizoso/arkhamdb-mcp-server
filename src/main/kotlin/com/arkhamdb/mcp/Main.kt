package com.arkhamdb.mcp

import com.arkhamdb.mcp.prompts.registerRulesPrompts
import com.arkhamdb.mcp.resources.registerResources
import com.arkhamdb.mcp.resources.registerPdfResources
import com.arkhamdb.mcp.tools.registerCampaignRulesTools
import com.arkhamdb.mcp.tools.registerCardFaqTools
import com.arkhamdb.mcp.tools.registerCardTools
import com.arkhamdb.mcp.tools.registerDecklistTools
import com.arkhamdb.mcp.tools.registerPackTools
import com.arkhamdb.mcp.tools.registerPdfTools
import com.arkhamdb.mcp.tools.registerSearchAllPdfsTools
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.StdioServerTransport
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
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
                version = BuildConstants.VERSION
            ),
            options = ServerOptions(
                capabilities = ServerCapabilities(
                    tools = ServerCapabilities.Tools(listChanged = false),
                    resources = ServerCapabilities.Resources(
                        subscribe = false,
                        listChanged = false
                    ),
                    prompts = ServerCapabilities.Prompts(listChanged = false)
                )
            ),
            instructions = """
                Eres un asistente experto en Arkham Horror: El Juego de Cartas (AH:LCG).

                REGLA CRÍTICA — SIEMPRE consulta las herramientas antes de responder preguntas de reglas:
                - `lookup_rule` → para buscar mecánicas concretas (agotado, preparado, cazador, acción, etc.)
                - `get_skill_test_steps` → para preguntas sobre el orden de resolución de pruebas de habilidad
                - `search_all_pdfs` → para búsquedas amplias en reglamento + FAQ simultáneamente
                - `get_card` / `search_cards` → para verificar el texto exacto de una carta
                - `verify_restriction` → OBLIGATORIO antes de afirmar que algo "no puede" o "no está permitido"

                NUNCA respondas de memoria. El texto exacto del reglamento es la única fuente de verdad.

                GUARDIA DE RESTRICCIONES: Si no encuentras texto explícito que prohíba algo, NO lo prohíbas.
                Las restricciones implícitas no existen en AH:LCG.

                ADVERTENCIA CROSS-GAME: AH:LCG funciona distinto a MTG, Hearthstone, etc.
                "Agotado" ≠ "tapped". No asumas comportamientos de otros juegos.

                CITAS OBLIGATORIAS: Cada restricción o permiso debe ir respaldado por el texto exacto de la regla.
            """.trimIndent()
        )

        // 3. Register all tools
        logger.info("Registering tools...")
        registerCardTools(server, arkhamClient)
        registerPackTools(server, arkhamClient)
        registerDecklistTools(server, arkhamClient)
        registerPdfTools(server)
        registerSearchAllPdfsTools(server)
        registerCardFaqTools(server, arkhamClient)
        registerCampaignRulesTools(server, arkhamClient)

        // 4. Register prompts
        logger.info("Registering prompts...")
        registerRulesPrompts(server)

        // 5. Register all resources
        logger.info("Registering resources...")
        registerResources(server, arkhamClient)
        registerPdfResources(server)

        // 6. Setup STDIO transport
        logger.info("Setting up STDIO transport...")
        val transport = StdioServerTransport(
            inputStream = System.`in`.asSource().buffered(),
            outputStream = System.out.asSink().buffered()
        )

        // 7. Create session and run
        logger.info("Creating server session...")
        server.createSession(transport)

        logger.info("ArkhamDB MCP Server is running and ready to accept requests")

        // Keep the session running indefinitely
        awaitCancellation()
    } catch (e: Exception) {
        logger.error("Fatal error in ArkhamDB MCP Server", e)
        throw e
    }
}
