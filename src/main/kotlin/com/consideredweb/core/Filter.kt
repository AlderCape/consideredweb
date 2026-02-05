package com.consideredweb.core

import org.slf4j.LoggerFactory
import java.util.UUID

private val logger = LoggerFactory.getLogger("com.consideredweb.core.Filters")

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
     * Logging filter - logs request method/path and response status with duration
     */
    fun logging(): Filter = filter { handler: HttpHandler ->
        HttpHandler { request: Request ->
            logger.debug("{} {}", request.method, request.uri)
            val start = System.currentTimeMillis()
            val response = handler.handle(request)
            val duration = System.currentTimeMillis() - start
            logger.info("{} {} -> {} ({}ms)", request.method, request.uri, response.status, duration)
            response
        }
    }

    /**
     * Error handling filter - catches exceptions and returns JSON error responses
     */
    fun errorHandling(): Filter = filter { handler: HttpHandler ->
        HttpHandler { request: Request ->
            try {
                handler.handle(request)
            } catch (e: Exception) {
                logger.error("Error handling request: {} {} - {}", request.method, request.uri, e.message, e)

                // Return JSON error response
                val errorJson = """{"message": "Internal server error: ${e.message}", "code": "INTERNAL_ERROR"}"""
                HttpResponse.internalServerError(errorJson)
                    .withHeader("Content-Type", "application/json")
            }
        }
    }

    /**
     * Detailed request/response logging filter.
     *
     * - DEBUG: method, path, status, headers
     * - TRACE: request/response bodies (truncated to 200 chars)
     *
     * @param logHeaders Whether to log headers (default: true)
     * @param logBody Whether to log bodies at TRACE level (default: false)
     * @param maxBodyLength Maximum body length to log (default: 200)
     */
    fun requestLogging(
        logHeaders: Boolean = true,
        logBody: Boolean = false,
        maxBodyLength: Int = 200
    ): Filter = filter { handler: HttpHandler ->
        HttpHandler { request: Request ->
            logger.debug("REQUEST: {} {}", request.method, request.uri)
            if (logHeaders) {
                logger.debug("REQUEST Headers: {}", request.headers)
            }
            if (logBody && logger.isTraceEnabled) {
                logger.trace("REQUEST Body: {}", request.body?.take(maxBodyLength))
            }

            val response = handler.handle(request)

            logger.debug("RESPONSE: {} {} -> Status: {}", request.method, request.uri, response.status)
            if (logHeaders) {
                logger.debug("RESPONSE Headers: {}", response.headers)
            }
            if (logBody && logger.isTraceEnabled) {
                logger.trace("RESPONSE Body: {}", response.body?.take(maxBodyLength))
            }

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

    /**
     * Correlation ID filter - adds request tracing support.
     *
     * Propagates incoming correlation ID from request headers or generates a new one.
     * Adds the correlation ID to the response headers and makes it available on the Request object.
     *
     * @param config Configuration for correlation ID behavior
     */
    fun correlationId(config: CorrelationIdConfig = CorrelationIdConfig()): Filter = filter { handler: HttpHandler ->
        HttpHandler { request: Request ->
            // Look for existing correlation ID in configured headers
            val existingId = config.requestHeaders
                .firstNotNullOfOrNull { headerName -> request.header(headerName) }

            // Use existing or generate new
            val correlationId = existingId ?: config.generator()

            logger.debug("Correlation ID: {} (propagated: {})", correlationId, existingId != null)

            // Add correlation ID to request
            val requestWithCorrelationId = request.copy(correlationId = correlationId)

            // Handle request
            val response = handler.handle(requestWithCorrelationId)

            // Add correlation ID to response header
            response.withHeader(config.responseHeader, correlationId)
        }
    }
}

/**
 * Configuration for correlation ID filter.
 *
 * @param requestHeaders Headers to check for incoming correlation ID (checked in order)
 * @param responseHeader Header name for the correlation ID in the response
 * @param generator Function to generate a new correlation ID when none is provided
 */
data class CorrelationIdConfig(
    val requestHeaders: List<String> = listOf("X-Correlation-ID", "X-Request-ID", "X-Trace-ID"),
    val responseHeader: String = "X-Correlation-ID",
    val generator: () -> String = { UUID.randomUUID().toString() }
)

/**
 * Extension function to add headers to HttpResponse
 */
fun HttpResponse.withHeader(name: String, value: String): HttpResponse {
    return this.copy(headers = this.headers + (name to value))
}

fun HttpResponse.withHeaders(newHeaders: Map<String, String>): HttpResponse {
    return this.copy(headers = this.headers + newHeaders)
}