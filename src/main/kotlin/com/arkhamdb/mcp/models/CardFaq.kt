package com.arkhamdb.mcp.models

import kotlinx.serialization.Serializable

@Serializable
data class CardFaq(
    val code: String? = null,
    val text: String? = null,
    val html: String? = null
)
