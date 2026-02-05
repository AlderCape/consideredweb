package com.consideredweb

import com.consideredweb.core.*
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FilterTest {

    @Test
    fun `should apply CORS filter`() {
        val routes = buildRouter {
            filter(Filters.cors(allowedOrigins = listOf("http://localhost:3000")))

            get("/test") {
                HttpResponse.ok("Test response")
            }
        }

        val handler = EnhancedFrameworkHandler(routes)

        // Test regular request
        val request = Request(
            method = "GET",
            uri = "/test",
            path = "/test",
            queryParams = emptyMap(),
            pathParams = emptyMap(),
            headers = mapOf("Origin" to "http://localhost:3000"),
            body = "",
            remoteAddress = "127.0.0.1"
        )

        val response = handler.handle(request)
        assertEquals(200, response.status)
        assertEquals("http://localhost:3000", response.headers["Access-Control-Allow-Origin"])
        assertEquals("true", response.headers["Access-Control-Allow-Credentials"])
    }

    @Test
    fun `should handle CORS preflight request`() {
        val routes = buildRouter {
            filter(Filters.cors(allowedOrigins = listOf("http://localhost:3000")))

            get("/test") {
                HttpResponse.ok("Test response")
            }

            // Add an explicit OPTIONS route to match
            options("/test") {
                HttpResponse.ok("Options response")
            }
        }

        val handler = EnhancedFrameworkHandler(routes)

        // Test OPTIONS request
        val request = Request(
            method = "OPTIONS",
            uri = "/test",
            path = "/test",
            queryParams = emptyMap(),
            pathParams = emptyMap(),
            headers = mapOf("Origin" to "http://localhost:3000"),
            body = "",
            remoteAddress = "127.0.0.1"
        )

        val response = handler.handle(request)
        assertEquals(200, response.status)
        assertEquals("http://localhost:3000", response.headers["Access-Control-Allow-Origin"])
        assertTrue(response.headers.containsKey("Access-Control-Allow-Methods"))
        assertTrue(response.headers.containsKey("Access-Control-Allow-Headers"))
    }

    @Test
    fun `should chain multiple filters`() {
        var loggingCalled = false
        val customLogging = filter { handler: HttpHandler ->
            HttpHandler { request: Request ->
                loggingCalled = true
                handler.handle(request)
            }
        }

        val routes = buildRouter {
            filter(customLogging)
            filter(Filters.errorHandling())

            get("/test") {
                HttpResponse.ok("Test response")
            }
        }

        val handler = EnhancedFrameworkHandler(routes)

        val request = Request(
            method = "GET",
            uri = "/test",
            path = "/test",
            queryParams = emptyMap(),
            pathParams = emptyMap(),
            headers = emptyMap(),
            body = "",
            remoteAddress = "127.0.0.1"
        )

        val response = handler.handle(request)
        assertEquals(200, response.status)
        assertTrue(loggingCalled)
    }

    @Test
    fun `should support route groups with filters`() {
        // Create auth filter outside the builder
        val authFilter = filter { handler: HttpHandler ->
            HttpHandler { request: Request ->
                // Simulate auth check
                if (request.header("Authorization") == null) {
                    HttpResponse.unauthorized("Authorization required")
                } else {
                    handler.handle(request)
                }
            }
        }

        val routes = buildRouter {
            // Public routes
            get("/health") {
                HttpResponse.ok("OK")
            }

            // Protected routes with auth filter
            group("/api", authFilter) {
                get("/users") {
                    HttpResponse.okJson(listOf("user1", "user2"))
                }

                get("/profile") {
                    HttpResponse.okJson(mapOf("name" to "John"))
                }
            }
        }

        val handler = EnhancedFrameworkHandler(routes)

        // Test public route
        val publicRequest = Request(
            method = "GET",
            uri = "/health",
            path = "/health",
            queryParams = emptyMap(),
            pathParams = emptyMap(),
            headers = emptyMap(),
            body = "",
            remoteAddress = "127.0.0.1"
        )

        val publicResponse = handler.handle(publicRequest)
        assertEquals(200, publicResponse.status)

        // Test protected route without auth
        val unauthedRequest = Request(
            method = "GET",
            uri = "/api/users",
            path = "/api/users",
            queryParams = emptyMap(),
            pathParams = emptyMap(),
            headers = emptyMap(),
            body = "",
            remoteAddress = "127.0.0.1"
        )

        val unauthedResponse = handler.handle(unauthedRequest)
        assertEquals(401, unauthedResponse.status)

        // Test protected route with auth
        val authedRequest = Request(
            method = "GET",
            uri = "/api/users",
            path = "/api/users",
            queryParams = emptyMap(),
            pathParams = emptyMap(),
            headers = mapOf("Authorization" to "Bearer token"),
            body = "",
            remoteAddress = "127.0.0.1"
        )

        val authedResponse = handler.handle(authedRequest)
        assertEquals(200, authedResponse.status)
    }

    @Test
    fun `binary response should be preserved through filter chain`() {
        val binaryData = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) // PNG header

        val routes = buildRouter {
            filter(Filters.correlationId())
            filter(Filters.cors())

            get("/image") {
                HttpResponse.okBinary(binaryData, "image/png")
            }
        }

        val handler = EnhancedFrameworkHandler(routes)

        val request = Request(
            method = "GET",
            uri = "/image",
            path = "/image",
            queryParams = emptyMap(),
            pathParams = emptyMap(),
            headers = emptyMap(),
            body = "",
            remoteAddress = "127.0.0.1"
        )

        val response = handler.handle(request)
        assertEquals(200, response.status)
        assertEquals("image/png", response.contentType)
        assertNotNull(response.bodyBytes, "bodyBytes should not be null for binary response")
        assertTrue(response.bodyBytes!!.contentEquals(binaryData), "Binary data should be preserved")
        assertTrue(response.isBinary, "Response should be marked as binary")
    }

    @Test
    fun `binary response should be preserved when adding headers`() {
        val binaryData = byteArrayOf(1, 2, 3, 4, 5)

        val response = HttpResponse.okBinary(binaryData, "application/octet-stream")
            .withHeader("X-Custom", "value")
            .withHeader("X-Another", "header")

        assertNotNull(response.bodyBytes, "bodyBytes should be preserved after withHeader")
        assertTrue(response.bodyBytes!!.contentEquals(binaryData), "Binary data should match")
        assertEquals("value", response.headers["X-Custom"])
        assertEquals("header", response.headers["X-Another"])
    }

    @Test
    fun `correlation ID filter should generate ID when not provided`() {
        val routes = buildRouter {
            filter(Filters.correlationId())

            get("/test") { request ->
                // Verify correlation ID is available in handler
                assertNotNull(request.correlationId)
                HttpResponse.ok("OK")
            }
        }

        val handler = EnhancedFrameworkHandler(routes)

        val request = Request(
            method = "GET",
            uri = "/test",
            path = "/test",
            queryParams = emptyMap(),
            pathParams = emptyMap(),
            headers = emptyMap(),
            body = "",
            remoteAddress = "127.0.0.1"
        )

        val response = handler.handle(request)
        assertEquals(200, response.status)
        assertNotNull(response.headers["X-Correlation-ID"], "Response should have correlation ID header")
    }

    @Test
    fun `correlation ID filter should propagate existing ID`() {
        val existingCorrelationId = "existing-correlation-id-123"

        val routes = buildRouter {
            filter(Filters.correlationId())

            get("/test") { request ->
                assertEquals(existingCorrelationId, request.correlationId)
                HttpResponse.ok("OK")
            }
        }

        val handler = EnhancedFrameworkHandler(routes)

        val request = Request(
            method = "GET",
            uri = "/test",
            path = "/test",
            queryParams = emptyMap(),
            pathParams = emptyMap(),
            headers = mapOf("X-Correlation-ID" to existingCorrelationId),
            body = "",
            remoteAddress = "127.0.0.1"
        )

        val response = handler.handle(request)
        assertEquals(200, response.status)
        assertEquals(existingCorrelationId, response.headers["X-Correlation-ID"])
    }

    @Test
    fun `correlation ID filter should support custom configuration`() {
        val customId = "custom-trace-123"

        val routes = buildRouter {
            filter(Filters.correlationId(CorrelationIdConfig(
                requestHeaders = listOf("X-My-Trace"),
                responseHeader = "X-My-Trace",
                generator = { "generated-id" }
            )))

            get("/test") { request ->
                HttpResponse.ok("OK")
            }
        }

        val handler = EnhancedFrameworkHandler(routes)

        // Test with custom header
        val requestWithHeader = Request(
            method = "GET",
            uri = "/test",
            path = "/test",
            queryParams = emptyMap(),
            pathParams = emptyMap(),
            headers = mapOf("X-My-Trace" to customId),
            body = "",
            remoteAddress = "127.0.0.1"
        )

        val responseWithHeader = handler.handle(requestWithHeader)
        assertEquals(customId, responseWithHeader.headers["X-My-Trace"])

        // Test with generator
        val requestWithoutHeader = Request(
            method = "GET",
            uri = "/test",
            path = "/test",
            queryParams = emptyMap(),
            pathParams = emptyMap(),
            headers = emptyMap(),
            body = "",
            remoteAddress = "127.0.0.1"
        )

        val responseWithGenerator = handler.handle(requestWithoutHeader)
        assertEquals("generated-id", responseWithGenerator.headers["X-My-Trace"])
    }
}