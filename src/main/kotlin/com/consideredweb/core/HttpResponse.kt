package com.consideredweb.core

data class HttpResponse(
    val status: Int,
    val body: String,
    val contentType: String = "application/json",
    val headers: Map<String, String> = emptyMap()
) {
    companion object {
        fun ok(body: String, contentType: String = "application/json") =
            HttpResponse(200, body, contentType)

        fun created(body: String, contentType: String = "application/json") =
            HttpResponse(201, body, contentType)

        fun noContent() = HttpResponse(204, "")

        fun badRequest(body: String) = HttpResponse(400, body)

        fun unauthorized(body: String) = HttpResponse(401, body)

        fun forbidden(body: String) = HttpResponse(403, body)

        fun notFound(body: String) = HttpResponse(404, body)

        fun internalServerError(body: String) = HttpResponse(500, body)
    }
}