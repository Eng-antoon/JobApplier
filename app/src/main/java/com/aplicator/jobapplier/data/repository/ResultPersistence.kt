package com.aplicator.jobapplier.data.repository

internal suspend fun <T> Result<T>.requireSuccessfulPersistence(
    persist: suspend (T) -> Result<Unit>,
): Result<T> = mapCatching { value ->
    persist(value).getOrThrow()
    value
}
