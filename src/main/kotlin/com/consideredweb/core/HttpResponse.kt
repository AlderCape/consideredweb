package com.consideredweb.core

data class HttpResponse(
    val status: Int,
    val body: String,
    val contentType: String = "application/json",
    val headers: Map<String, String> = emptyMap(),
    val bodyBytes: ByteArray? = null
) {
    /**
     * Whether this response contains binary data
     */
    val isBinary: Boolean get() = bodyBytes != null

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

        /**
         * Create a binary response with the given bytes and content type
         */
        fun okBinary(bytes: ByteArray, contentType: String) =
            HttpResponse(200, "", contentType, emptyMap(), bytes)

        /**
         * Create a binary response with custom status code
         */
        fun binary(bytes: ByteArray, contentType: String, status: Int = 200) =
            HttpResponse(status, "", contentType, emptyMap(), bytes)
    }

    /**
     * Add a header to the response
     */
    fun withHeader(name: String, value: String): HttpResponse =
        copy(headers = headers + (name to value))

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as HttpResponse

        if (status != other.status) return false
        if (body != other.body) return false
        if (contentType != other.contentType) return false
        if (headers != other.headers) return false
        if (bodyBytes != null) {
            if (other.bodyBytes == null) return false
            if (!bodyBytes.contentEquals(other.bodyBytes)) return false
        } else if (other.bodyBytes != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = status
        result = 31 * result + body.hashCode()
        result = 31 * result + contentType.hashCode()
        result = 31 * result + headers.hashCode()
        result = 31 * result + (bodyBytes?.contentHashCode() ?: 0)
        return result
    }
}