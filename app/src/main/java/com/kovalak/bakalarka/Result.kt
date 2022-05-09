package com.kovalak.bakalarka

sealed class Result {
    class Success(val key: ByteArray) : Result()
    class Failure(val message: String) : Result()
    object Processing : Result()
}