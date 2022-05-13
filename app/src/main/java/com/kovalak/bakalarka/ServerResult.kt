package com.kovalak.bakalarka

import kotlinx.serialization.Serializable

@Serializable
class ServerResult(
    val hasFailed: Boolean,
    val message: String,
    val key: ByteArray? = null,
)