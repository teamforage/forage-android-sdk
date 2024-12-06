package com.joinforage.forage.android.core.services.forageapi.engine

/**
 * An error for the case when the request fails and the server
 * never responded
 */
internal class HttpRequestFailedException(cause: Exception) : Exception(cause)
