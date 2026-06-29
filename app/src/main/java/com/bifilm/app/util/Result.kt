package com.bifilm.app.util

sealed interface BiFilmResult<out T> {
    data class Ok<T>(val value: T) : BiFilmResult<T>
    data class Err(val error: Throwable) : BiFilmResult<Nothing>
}

inline fun <T> runCatchingBF(block: () -> T): BiFilmResult<T> =
    try {
        BiFilmResult.Ok(block())
    } catch (t: Throwable) {
        BiFilmResult.Err(t)
    }
