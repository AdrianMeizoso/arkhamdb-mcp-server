package com.arkhamdb.mcp.resources

import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.types.ReadResourceResult
import io.modelcontextprotocol.kotlin.sdk.types.TextResourceContents
import kotlinx.coroutines.runBlocking
import org.apache.pdfbox.Loader
import org.apache.pdfbox.text.PDFTextStripper
import org.slf4j.LoggerFactory
import java.io.File

private val logger = LoggerFactory.getLogger("PdfResources")

/**
 * Data class representing a PDF resource configuration
 */
data class PdfResourceConfig(
    val uri: String,
    val name: String,
    val description: String,
    val fileName: String
)

/**
 * Helper function to load and extract text from a PDF file
 */
private fun loadPdfText(pdfPath: String): String {
    // Try to load from resources first
    val resourceStream = PdfResourceConfig::class.java.classLoader.getResourceAsStream(pdfPath)

    return if (resourceStream != null) {
        logger.info("Loading PDF from resources: $pdfPath")
        resourceStream.use { stream ->
            val document = Loader.loadPDF(stream.readBytes())
            val stripper = PDFTextStripper()
            val text = stripper.getText(document)
            document.close()
            text
        }
    } else {
        // Fallback to external file path
        val externalPath = File("src/main/resources/$pdfPath")
        if (externalPath.exists()) {
            logger.info("Loading PDF from file: ${externalPath.absolutePath}")
            val document = Loader.loadPDF(externalPath)
            val stripper = PDFTextStripper()
            val text = stripper.getText(document)
            document.close()
            text
        } else {
            throw IllegalStateException(
                "PDF not found. Please place '${externalPath.name}' in src/main/resources/pdfs/"
            )
        }
    }
}

/**
 * Register a single PDF resource
 */
private fun registerPdfResource(server: Server, config: PdfResourceConfig) {
    server.addResource(
        uri = config.uri,
        name = config.name,
        description = config.description,
        mimeType = "text/plain"
    ) { request ->
        runBlocking {
            logger.info("Reading ${config.name}")

            try {
                val pdfPath = "pdfs/${config.fileName}"
                val text = loadPdfText(pdfPath)

                logger.info("Successfully extracted ${text.length} characters from ${config.fileName}")

                ReadResourceResult(
                    contents = listOf(
                        TextResourceContents(
                            text = text,
                            uri = request.uri,
                            mimeType = "text/plain"
                        )
                    )
                )
            } catch (e: Exception) {
                logger.error("Error reading PDF: ${config.fileName}", e)
                ReadResourceResult(
                    contents = listOf(
                        TextResourceContents(
                            text = "Error reading PDF: ${e.message}",
                            uri = request.uri,
                            mimeType = "text/plain"
                        )
                    )
                )
            }
        }
    }
}

fun registerPdfResources(server: Server) {
    // Define all PDF resources
    val pdfResources = listOf(
        PdfResourceConfig(
            uri = "arkhamdb://rules/pdf",
            name = "Arkham Horror LCG Rules",
            description = "Complete rules reference for Arkham Horror: The Card Game",
            fileName = "arkham_horror_rules.pdf"
        ),
        PdfResourceConfig(
            uri = "arkhamdb://rules/faq",
            name = "Arkham Horror LCG FAQ",
            description = "Frequently Asked Questions and official clarifications for Arkham Horror: The Card Game",
            fileName = "arkham_horror_faq.pdf"
        )
    )

    // Register each PDF resource
    pdfResources.forEach { config ->
        registerPdfResource(server, config)
    }

    logger.info("Registered ${pdfResources.size} PDF resources")
}
