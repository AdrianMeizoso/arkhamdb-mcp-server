package com.arkhamdb.mcp.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class DeckRequirements(
    val size: Int? = null,
    val card: JsonObject? = null,
    val random: List<JsonObject>? = null
)

@Serializable
data class DeckOption(
    val faction: List<String>? = null,
    val level: JsonObject? = null,
    val trait: List<String>? = null,
    val tag: List<String>? = null,
    val type: List<String>? = null,
    val limit: Int? = null,
    val error: String? = null,
    val not: Boolean? = null
)

@Serializable
data class Restrictions(
    val investigator: JsonObject? = null
)

@Serializable
data class Card(
    val code: String,
    val name: String,
    val real_name: String? = null,
    val type_code: String,
    val type_name: String,
    val faction_code: String,
    val faction_name: String,
    val pack_code: String,
    val pack_name: String,
    val text: String? = null,
    val cost: Int? = null,
    val xp: Int? = null,
    val traits: String? = null,
    val skill_willpower: Int? = null,
    val skill_intellect: Int? = null,
    val skill_combat: Int? = null,
    val skill_agility: Int? = null,
    val health: Int? = null,
    val sanity: Int? = null,
    val deck_limit: Int? = null,
    val slot: String? = null,
    val deck_requirements: DeckRequirements? = null,
    val deck_options: List<DeckOption>? = null,
    val flavor: String? = null,
    val illustrator: String? = null,
    val position: Int? = null,
    val quantity: Int? = null,
    val restrictions: Restrictions? = null,
    val subtype_code: String? = null,
    val subtype_name: String? = null,
    val url: String? = null,
    val encounter_code: String? = null,
    val encounter_name: String? = null,
    val encounter_position: Int? = null,
    val linked_card: Card? = null
)
