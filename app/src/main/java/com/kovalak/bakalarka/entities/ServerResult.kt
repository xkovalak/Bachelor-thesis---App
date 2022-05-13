package com.kovalak.bakalarka.entities

import kotlinx.serialization.Serializable

@Serializable
class ServerResult(
    val hasFailed: Boolean,
    val message: String,
    val key: ByteArray? = null,
)