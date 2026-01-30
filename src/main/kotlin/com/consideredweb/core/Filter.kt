package com.consideredweb.core

/**
 * Filter interface for middleware functionality.
 *
 * Filters can intercept requests and responses to add cross-cutting
 * concerns like authentication, logging, CORS, etc.
 */
fun interface Filter {
    /**
     * Apply this filter to a handler.
     *
     * @param handler The next handler in the chain
     * @return A new handler that applies this filter
     */
    fun filter(handler: HttpHandler): HttpHandler
}

/**
 * Extension function to chain filters together
 */
fun Filter.then(handler: HttpHandler): HttpHandler {
    return this.filter(handler)
}

/**
 * Extension function to chain multiple filters
 */
fun Filter.then(nextFilter: Filter): Filter {
    return Filter { handler -> this.filter(nextFilter.filter(handler)) }
}

/**
 * Utility to create a filter from a lambda
 */
fun filter(block: (HttpHandler) -> HttpHandler): Filter = Filter(block)

/**
 * Common filter implementations
 */
object Filters {

    /**
     * CORS filter with configurable origins
     */
    fun cors(
        allowedOrigins: List<String> = listOf("*"),
        allowedMethods: List<String> = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"),
        allowedHeaders: List<String> = listOf("content-type", "authorization"),
        allowCredentials: Boolean = true
    ): Filter = filter { handler: HttpHandler ->
        HttpHandler { request: Request ->
            val origin = request.headers["Origin"]

            if (request.method == "OPTIONS") {
                // Handle preflight requests
                HttpResponse.ok("")
                    .withHeader("Access-Control-Allow-Origin", getOriginHeader(origin, allowedOrigins))
                    .withHeader("Access-Control-Allow-Methods", allowedMethods.joinToString(", "))
                    .withHeader("Access-Control-Allow-Headers", allowedHeaders.joinToString(", "))
                    .withHeader("Access-Control-Allow-Credentials", allowCredentials.toString())
                    .withHeader("Access-Control-Max-Age", "86400")
            } else {
                val response = handler.handle(request)
                response
                    .withHeader("Access-Control-Allow-Origin", getOriginHeader(origin, allowedOrigins))
                    .withHeader("Access-Control-Allow-Credentials", allowCredentials.toString())
            }
        }
    }

    /**
     * Logging filter
     */
    fun logging(): Filter = filter { handler: HttpHandler ->
        HttpHandler { request: Request ->
            println("${request.method} ${request.uri}")
            val start = System.currentTimeMillis()
            val response = handler.handle(request)
            val duration = System.currentTimeMillis() - start
            println("${request.method} ${request.uri} -> ${response.status} (${duration}ms)")
            response
        }
    }

    /**
     * Error handling filter
     */
    fun errorHandling(): Filter = filter { handler: HttpHandler ->
        HttpHandler { request: Request ->
            try {
                val response = handler.handle(request)
                println("🔍 RESPONSE DEBUG: ${request.method} ${request.uri} -> Status: ${response.status}, Content-Type: ${response.headers["Content-Type"]}, Body: ${response.body?.take(200)}")
                response
            } catch (e: Exception) {
                println("🚨 ERROR handling request: ${request.method} ${request.uri} - ${e.message}")
                e.printStackTrace()

                // Return JSON error response
                val errorJson = """{"message": "Internal server error: ${e.message}", "code": "INTERNAL_ERROR"}"""
                val errorResponse = HttpResponse.internalServerError(errorJson)
                    .withHeader("Content-Type", "application/json")
                println("🔍 ERROR RESPONSE: Status: ${errorResponse.status}, Content-Type: ${errorResponse.headers["Content-Type"]}, Body: ${errorResponse.body}")
                errorResponse
            }
        }
    }

    /**
     * Request/Response logging filter
     */
    fun requestLogging(): Filter = filter { handler: HttpHandler ->
        HttpHandler { request: Request ->
            println("📥 REQUEST: ${request.method} ${request.uri}")
            println("📥 REQUEST Headers: ${request.headers}")
            println("📥 REQUEST Body: ${request.body?.take(500)}")

            val response = handler.handle(request)

            println("📤 RESPONSE: ${request.method} ${request.uri} -> Status: ${response.status}")
            println("📤 RESPONSE Headers: ${response.headers}")
            println("📤 RESPONSE Body: ${response.body?.take(500)}")

            response
        }
    }

    private fun getOriginHeader(origin: String?, allowedOrigins: List<String>): String {
        return when {
            allowedOrigins.contains("*") -> "*"
            origin != null && allowedOrigins.contains(origin) -> origin
            allowedOrigins.isNotEmpty() -> allowedOrigins.first()
            else -> "*"
        }
    }
}

/**
 * Extension function to add headers to HttpResponse
 */
fun HttpResponse.withHeader(name: String, value: String): HttpResponse {
    return this.copy(headers = this.headers + (name to value))
}

fun HttpResponse.withHeaders(newHeaders: Map<String, String>): HttpResponse {
    return this.copy(headers = this.headers + newHeaders)
}