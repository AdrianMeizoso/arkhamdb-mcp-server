package com.arkhamdb.mcp.tools

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("PdfTools")

fun registerPdfTools(server: Server) {
    // Tool: get_rules
    server.addTool(
        name = "get_rules",
        description = """Get the official rules for Arkham Horror: The Card Game from the Rules Reference PDF.
Use this tool when the user asks about:
- How to play, game rules, mechanics, or procedures
- Specific game concepts (actions, reactions, triggers, keywords, etc.)
- Setup or scenario instructions
- Card interactions or rule clarifications
Optionally provide a 'query' to search for a specific topic within the rules.
IMPORTANT: Las reglas están en español. Usar términos en español en la búsqueda (e.g., 'perdición', 'acción', 'evadir', 'prueba de habilidad', 'horror', 'daño').""",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("query", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Término o tema a buscar en las reglas (en español, e.g., 'evadir', 'acción', 'perdición', 'prueba de habilidad')"))
                })
            }
        )
    ) { request ->
        val query = request.params.arguments.string("query")

        logger.info("get_rules called, query: $query")

        try {
            val fullText = withContext(Dispatchers.IO) { PdfCache.load("arkham_horror_rules.pdf") }
            val result = if (query != null) {
                logger.info("Searching rules for: $query")
                val searchResult = PdfCache.search(fullText, query)
                "## Rules Reference — Search: \"$query\"\n\n$searchResult"
            } else {
                "## Arkham Horror LCG — Rules Reference\n\n$fullText"
            }

            CallToolResult(content = listOf(TextContent(text = result)))
        } catch (e: Exception) {
            logger.error("Error loading rules PDF", e)
            CallToolResult(
                content = listOf(TextContent(text = "Error loading rules: ${e.message}")),
                isError = true
            )
        }
    }

    // Tool: lookup_rule
    server.addTool(
        name = "lookup_rule",
        description = """Look up a specific game mechanic or keyword in the official Arkham Horror LCG rules and FAQ.
Use this tool proactively whenever answering questions about card interactions or rules, to verify the exact ruling before responding.
Typical use cases:
- Verifying how a keyword works (e.g. 'agotado', 'preparado', 'cazador', 'descomunal', 'rápido')
- Checking timing rules for triggered effects ([acción], [reacción], [libre])
- Confirming what 'mover', 'colocar', 'revelar', 'derrotar' mean precisely
- Clarifying when effects apply (e.g. 'durante', 'después de', 'antes de')
Searches both the Rules Reference PDF and the FAQ PDF.
IMPORTANT: Use Spanish terms (e.g. 'agotado' not 'exhausted', 'preparado' not 'ready', 'cazador' not 'hunter').""",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("term", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Término de regla o mecánica a buscar (en español, e.g., 'agotado', 'preparado', 'cazador', 'descomunal', 'prueba de habilidad')"))
                })
            },
            required = listOf("term")
        )
    ) { request ->
        val term = request.params.arguments.string("term") ?: return@addTool CallToolResult(
            content = listOf(TextContent(text = "El parámetro 'term' es obligatorio.")),
            isError = true
        )

        logger.info("lookup_rule called, term: $term")

        try {
            val rulesText = withContext(Dispatchers.IO) { PdfCache.load("arkham_horror_rules.pdf") }
            val faqText = withContext(Dispatchers.IO) { PdfCache.loadOrNull("arkham_horror_faq.pdf") }

            val rulesResult = PdfCache.search(rulesText, term)
            val faqResult = faqText?.let { PdfCache.search(it, term) }

            val sb = StringBuilder()
            sb.appendLine("## Lookup: \"$term\"")
            sb.appendLine()
            sb.appendLine("### Reglamento")
            sb.appendLine(rulesResult)
            if (!faqResult.isNullOrBlank()) {
                sb.appendLine()
                sb.appendLine("### FAQ & Errata")
                sb.appendLine(faqResult)
            }

            CallToolResult(content = listOf(TextContent(text = sb.toString())))
        } catch (e: Exception) {
            logger.error("Error in lookup_rule", e)
            CallToolResult(
                content = listOf(TextContent(text = "Error buscando '$term': ${e.message}")),
                isError = true
            )
        }
    }

    // Tool: get_skill_test_steps
    server.addTool(
        name = "get_skill_test_steps",
        description = """Get the official step-by-step procedure for resolving a skill test in Arkham Horror LCG.
Use this tool proactively whenever answering questions about card interactions during a skill test, to verify the exact timing and order of effects.
Typical use cases:
- Understanding when committed skill card effects trigger (before/after chaos token reveal)
- Determining the order of damage application vs. card effects
- Clarifying when 'if successful' effects resolve relative to other effects
- Answering any question about what happens during a fight, evade, or investigate action""",
        inputSchema = ToolSchema(properties = buildJsonObject {})
    ) { _ ->
        logger.info("get_skill_test_steps called")

        try {
            val rulesText = withContext(Dispatchers.IO) { PdfCache.load("arkham_horror_rules.pdf") }
            val stepsResult = PdfCache.search(rulesText, "prueba de habilidad")
            val result = "## Pasos de la Prueba de Habilidad\n\n$stepsResult"
            CallToolResult(content = listOf(TextContent(text = result)))
        } catch (e: Exception) {
            logger.error("Error in get_skill_test_steps", e)
            CallToolResult(
                content = listOf(TextContent(text = "Error cargando los pasos de prueba de habilidad: ${e.message}")),
                isError = true
            )
        }
    }

    // Tool: verify_restriction
    server.addTool(
        name = "verify_restriction",
        description = """Verifica si una restricción o permiso específico está explícitamente respaldado por
el texto de las reglas. USA ESTA HERRAMIENTA antes de afirmar que algo "no puede"
o "no está permitido". Busca los términos clave en el reglamento y devuelve el texto
encontrado junto con una instrucción explícita: si el texto no menciona la restricción,
no la afirmes.""",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("claim", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("La restricción a verificar en forma de pregunta. Ej: \"¿Puede usarse una carta agotada para realizar su acción de combatir?\""))
                })
                put("terms", buildJsonObject {
                    put("type", JsonPrimitive("array"))
                    put("description", JsonPrimitive("Términos clave a buscar en el reglamento (en español). Ej: [\"agotado\", \"acción\"]"))
                    put("items", buildJsonObject { put("type", JsonPrimitive("string")) })
                })
            },
            required = listOf("claim", "terms")
        )
    ) { request ->
        val claim = request.params.arguments.string("claim") ?: return@addTool CallToolResult(
            content = listOf(TextContent(text = "El parámetro 'claim' es obligatorio.")),
            isError = true
        )
        val termsArray = request.params.arguments?.get("terms")?.jsonArray ?: return@addTool CallToolResult(
            content = listOf(TextContent(text = "El parámetro 'terms' es obligatorio.")),
            isError = true
        )
        val terms = termsArray.mapNotNull { it.jsonPrimitive.contentOrNull }.filter { it.isNotBlank() }
        if (terms.isEmpty()) return@addTool CallToolResult(
            content = listOf(TextContent(text = "'terms' no puede estar vacío.")),
            isError = true
        )

        logger.info("verify_restriction called, claim: $claim, terms: $terms")

        try {
            val rulesText = withContext(Dispatchers.IO) { PdfCache.load("arkham_horror_rules.pdf") }

            val sb = StringBuilder()
            sb.appendLine("## Verificación de restricción")
            sb.appendLine()
            sb.appendLine("**Afirmación a verificar:** $claim")
            sb.appendLine()

            for (term in terms) {
                val result = PdfCache.search(rulesText, term, contextWindow = 3, maxSections = 3)
                sb.appendLine("### Término: \"$term\"")
                sb.appendLine(result)
                sb.appendLine()
            }

            sb.appendLine("""
> INSTRUCCIÓN AL MODELO: Si el texto anterior no menciona explícitamente
> esta restricción, NO la afirmes en tu respuesta. La ausencia de texto
> que prohíba algo significa que está permitido.
            """.trimIndent())

            CallToolResult(content = listOf(TextContent(text = sb.toString())))
        } catch (e: Exception) {
            logger.error("Error in verify_restriction", e)
            CallToolResult(
                content = listOf(TextContent(text = "Error verificando restricción: ${e.message}")),
                isError = true
            )
        }
    }

    // Tool: get_faq
    server.addTool(
        name = "get_faq",
        description = """Get the official FAQ and errata for Arkham Horror: The Card Game.
Use this tool when the user asks about:
- Official rulings, clarifications, or errata on specific cards
- FAQs, frequently asked questions, or controversial rules
- Whether a card has been updated or changed in an errata
- Specific card interactions that may be in the FAQ
Optionally provide a 'query' to search for a specific card name or topic.
IMPORTANT: El FAQ está en español. Usar términos en español en la búsqueda (e.g., 'perdición', 'prueba de habilidad', 'evadir', 'acción gratuita').""",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                put("query", buildJsonObject {
                    put("type", JsonPrimitive("string"))
                    put("description", JsonPrimitive("Nombre de carta o tema a buscar en el FAQ (en español, e.g., 'Barricada', 'prueba de habilidad', 'perdición')"))
                })
            }
        )
    ) { request ->
        val query = request.params.arguments.string("query")

        logger.info("get_faq called, query: $query")

        try {
            val fullText = withContext(Dispatchers.IO) { PdfCache.load("arkham_horror_faq.pdf") }
            val result = if (query != null) {
                logger.info("Searching FAQ for: $query")
                val searchResult = PdfCache.search(fullText, query)
                "## FAQ & Errata — Search: \"$query\"\n\n$searchResult"
            } else {
                "## Arkham Horror LCG — FAQ & Errata\n\n$fullText"
            }

            CallToolResult(content = listOf(TextContent(text = result)))
        } catch (e: Exception) {
            logger.error("Error loading FAQ PDF", e)
            CallToolResult(
                content = listOf(TextContent(text = "Error loading FAQ: ${e.message}")),
                isError = true
            )
        }
    }
}
