package com.arkhamdb.mcp.tools

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

fun JsonObject?.string(key: String): String? = this?.get(key)?.jsonPrimitive?.contentOrNull
fun JsonObject?.boolean(key: String, default: Boolean = false): Boolean =
    this?.get(key)?.jsonPrimitive?.booleanOrNull ?: default
fun JsonObject?.int(key: String): Int? = this?.get(key)?.jsonPrimitive?.intOrNull
