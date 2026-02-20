package com.arkhamdb.mcp.models

import kotlinx.serialization.Serializable

@Serializable
data class Pack(
    val code: String,
    val name: String,
    val position: Int,
    val cycle_position: Int,
    val available: String,
    val known: Int,
    val total: Int,
    val url: String? = null,
    val id: Int? = null
)
