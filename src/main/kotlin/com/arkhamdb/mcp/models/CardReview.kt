package com.arkhamdb.mcp.models

import kotlinx.serialization.Serializable

@Serializable
data class CardReview(
    val id: Int? = null,
    val title: String? = null,
    val body: String,
    val author_username: String? = null,
    val date_creation: String? = null,
    val code: String? = null
)
