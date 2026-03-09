package com.arkhamdb.mcp.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Decklist(
    val id: Int,
    val name: String,
    val date_creation: String,
    val date_update: String,
    val description_md: String? = null,
    val user_id: Int? = null,
    val investigator_code: String,
    val investigator_name: String,
    val slots: Map<String, Int>,
    val sideSlots: JsonElement? = null,  // Can be [] (empty array) or {} (object with card mappings)
    val ignoreDeckLimitSlots: JsonElement? = null,  // Can be null, [] or {}

    val version: String? = null,
    val xp: Int? = null,
    val xp_spent: Int? = null,
    val xp_adjustment: Int? = null,
    val exile_string: String? = null,
    val tags: String? = null,
    val meta: String? = null,
    val url: String? = null
)
