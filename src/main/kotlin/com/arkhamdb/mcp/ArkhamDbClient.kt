package com.arkhamdb.mcp

import com.arkhamdb.mcp.models.Card
import com.arkhamdb.mcp.models.CardFaq
import com.arkhamdb.mcp.models.Decklist
import com.arkhamdb.mcp.models.Pack
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

/**
 * HTTP client for ArkhamDB API with caching support.
 */
class ArkhamDbClient {
    private val logger = LoggerFactory.getLogger(ArkhamDbClient::class.java)
    private val baseUrl = "https://es.arkhamdb.com/api/public"

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
                prettyPrint = true
            })
        }

        install(HttpCache)

        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 10000
        }

        defaultRequest {
            header("User-Agent", "ArkhamDB-MCP-Server/${BuildConstants.VERSION}")
        }
    }

    /**
     * Fetch all cards from the ArkhamDB API.
     */
    suspend fun getAllCards(): Result<List<Card>> = runCatching {
        logger.info("Fetching all cards from ArkhamDB API")
        val response = httpClient.get("$baseUrl/cards/")
        if (!response.status.isSuccess()) {
            throw IllegalStateException("API returned ${response.status.value} for cards/")
        }
        val cards: List<Card> = response.body()
        logger.info("Retrieved ${cards.size} cards")
        cards
    }.onFailure { error ->
        logger.error("Failed to fetch cards", error)
    }

    /**
     * Fetch a single card by its code.
     */
    suspend fun getCard(code: String): Result<Card> = runCatching {
        logger.info("Fetching card with code: $code")
        val response = httpClient.get("$baseUrl/card/$code")
        if (!response.status.isSuccess()) {
            throw IllegalStateException("API returned ${response.status.value} for card/$code")
        }
        val card: Card = response.body()
        logger.info("Retrieved card: ${card.name}")
        card
    }.onFailure { error ->
        logger.error("Failed to fetch card $code", error)
    }

    /**
     * Fetch all packs from the ArkhamDB API.
     */
    suspend fun getAllPacks(): Result<List<Pack>> = runCatching {
        logger.info("Fetching all packs from ArkhamDB API")
        val response = httpClient.get("$baseUrl/packs/")
        if (!response.status.isSuccess()) {
            throw IllegalStateException("API returned ${response.status.value} for packs/")
        }
        val packs: List<Pack> = response.body()
        logger.info("Retrieved ${packs.size} packs")
        packs
    }.onFailure { error ->
        logger.error("Failed to fetch packs", error)
    }

    /**
     * Fetch all cards including encounter cards from the ArkhamDB API.
     */
    suspend fun getAllCardsWithEncounter(): Result<List<Card>> = runCatching {
        logger.info("Fetching all cards (including encounter) from ArkhamDB API")
        val response = httpClient.get("$baseUrl/cards/") {
            parameter("encounter", "1")
        }
        if (!response.status.isSuccess()) {
            throw IllegalStateException("API returned ${response.status.value} for cards/?encounter=1")
        }
        val cards: List<Card> = response.body()
        logger.info("Retrieved ${cards.size} cards (with encounter)")
        cards
    }.onFailure { error ->
        logger.error("Failed to fetch cards with encounter", error)
    }

    /**
     * Fetch FAQ/rulings for a specific card by its code.
     */
    suspend fun getCardFaq(code: String): Result<List<CardFaq>> = runCatching {
        logger.info("Fetching FAQ for card: $code")
        val response: HttpResponse = httpClient.get("$baseUrl/faq/$code")
        if (!response.status.isSuccess()) {
            throw IllegalStateException("API returned ${response.status.value} for faq/$code")
        }
        val contentType = response.contentType()
        if (contentType?.contentType != "application" || contentType.contentSubtype != "json") {
            throw IllegalStateException("Unexpected content type for faq/$code: ${contentType ?: "unknown"}")
        }
        val faqs: List<CardFaq> = response.body()
        logger.info("Retrieved ${faqs.size} FAQ entries for card $code")
        faqs
    }.onFailure { error ->
        logger.error("Failed to fetch FAQ for card $code", error)
    }

    /**
     * Fetch a decklist by its ID.
     */
    suspend fun getDecklist(id: Int): Result<Decklist> = runCatching {
        logger.info("Fetching decklist with id: $id")
        val response: HttpResponse = httpClient.get("$baseUrl/decklist/$id")

        // Check if response is JSON (ArkhamDB returns HTML for non-existent decklists)
        val contentType = response.contentType()
        if (contentType?.contentType != "application" || contentType.contentSubtype != "json") {
            throw IllegalStateException("Decklist not found (API returned ${contentType ?: "unknown content type"})")
        }

        val decklist: Decklist = response.body()
        logger.info("Retrieved decklist: ${decklist.name}")
        decklist
    }.onFailure { error ->
        logger.error("Failed to fetch decklist $id", error)
    }

    /**
     * Close the HTTP client when done.
     */
    fun close() {
        httpClient.close()
    }
}
