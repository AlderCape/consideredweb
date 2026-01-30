package com.consideredweb.core

data class Request(
    val method: String,
    val uri: String,
    val path: String,
    val queryParams: Map<String, String>,
    val pathParams: Map<String, String>,
    val headers: Map<String, String>,
    val body: String,
    val remoteAddress: String
) {
    fun pathParam(name: String): String? = pathParams[name]
    fun queryParam(name: String): String? = queryParams[name]
    fun header(name: String): String? = headers[name]

    val contentType: String?
        get() = header("Content-Type")?.lowercase()?.split(";")?.first()?.trim()
}