package com.consideredweb

import com.consideredweb.core.*
import com.consideredweb.examples.HealthReport
import com.consideredweb.examples.MockHealthService
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for the ProjectVista route migration example.
 *
 * This shows how to test migrated routes using the same patterns
 * as the existing ConsideredWeb tests.
 */
class MigrationExampleTest {

    @Test
    fun `should handle basic health check`() {
        val routes = buildRouter {
            get("/health") {
                HttpResponse.ok("OK")
            }
        }

        val handler = EnhancedFrameworkHandler(routes)

        val request = Request(
            method = "GET",
            uri = "/health",
            path = "/health",
            queryParams = emptyMap(),
            pathParams = emptyMap(),
            headers = emptyMap(),
            body = "",
            remoteAddress = "127.0.0.1"
        )

        val response = handler.handle(request)
        assertEquals(200, response.status)
        assertEquals("OK", response.body)
    }

    @Test
    fun `should handle deep health check with token`() {
        val healthService = MockHealthService()

        val routes = buildRouter {
            get("/health/deep") { request ->
                val token = "test-token" // Simulating environment variable
                val allowed = request.header("X-Health-Token") == token

                if (!allowed) {
                    HttpResponse.forbidden("Forbidden")
                } else {
                    val report = healthService.check()
                    HttpResponse.okJson(report)
                }
            }
        }

        val handler = EnhancedFrameworkHandler(routes)

        // Test without token - should be forbidden
        val unauthorizedRequest = Request(
            method = "GET",
            uri = "/health/deep",
            path = "/health/deep",
            queryParams = emptyMap(),
            pathParams = emptyMap(),
            headers = emptyMap(),
            body = "",
            remoteAddress = "127.0.0.1"
        )

        val unauthorizedResponse = handler.handle(unauthorizedRequest)
        assertEquals(403, unauthorizedResponse.status)
        assertEquals("Forbidden", unauthorizedResponse.body)

        // Test with correct token - should succeed
        val authorizedRequest = Request(
            method = "GET",
            uri = "/health/deep",
            path = "/health/deep",
            queryParams = emptyMap(),
            pathParams = emptyMap(),
            headers = mapOf("X-Health-Token" to "test-token"),
            body = "",
            remoteAddress = "127.0.0.1"
        )

        val authorizedResponse = handler.handle(authorizedRequest)
        assertEquals(200, authorizedResponse.status)
        assertEquals("application/json", authorizedResponse.contentType)

        // Parse the JSON response to verify structure
        val healthReport = JsonSupport.fromJson<HealthReport>(authorizedResponse.body)
        assertEquals("healthy", healthReport.status)
        assertTrue(healthReport.checks.containsKey("database"))
        assertTrue(healthReport.checks.containsKey("blobStorage"))
    }

    @Test
    fun `should handle path parameters like ProjectVista`() {
        val routes = buildRouter {
            get("/api/organizations/{id}") { request ->
                val id = request.pathParamUuid("id")
                HttpResponse.okJson(mapOf(
                    "id" to id.toString(),
                    "name" to "Sample Organization"
                ))
            }
        }

        val handler = EnhancedFrameworkHandler(routes)
        val testUuid = UUID.randomUUID()

        val request = Request(
            method = "GET",
            uri = "/api/organizations/$testUuid",
            path = "/api/organizations/$testUuid",
            queryParams = emptyMap(),
            pathParams = mapOf("id" to testUuid.toString()),
            headers = emptyMap(),
            body = "",
            remoteAddress = "127.0.0.1"
        )

        val response = handler.handle(request)
        assertEquals(200, response.status)
        assertEquals("application/json", response.contentType)

        val result = JsonSupport.fromJson<Map<String, String>>(response.body)
        assertEquals(testUuid.toString(), result["id"])
        assertEquals("Sample Organization", result["name"])
    }

    @Test
    fun `should handle JSON request bodies like ProjectVista`() {
        @Serializable
        data class CreateOrgRequest(val name: String)

        val routes = buildRouter {
            post("/api/organizations") { request ->
                try {
                    val body = request.bodyAsJson<CreateOrgRequest>()
                    if (body.name.isBlank()) {
                        throw IllegalArgumentException("Name is required")
                    }

                    @Serializable
                    data class CreateOrgResponse(val id: String, val name: String, val created: Boolean)

                    HttpResponse.createdJson(CreateOrgResponse(
                        id = UUID.randomUUID().toString(),
                        name = body.name,
                        created = true
                    ))
                } catch (e: Exception) {
                    HttpResponse.badRequest("Invalid request: ${e.message}")
                }
            }
        }

        val handler = EnhancedFrameworkHandler(routes)

        // Test valid request
        val createRequest = CreateOrgRequest("Test Organization")
        val requestBody = JsonSupport.toJson(createRequest)

        val request = Request(
            method = "POST",
            uri = "/api/organizations",
            path = "/api/organizations",
            queryParams = emptyMap(),
            pathParams = emptyMap(),
            headers = mapOf("Content-Type" to "application/json"),
            body = requestBody,
            remoteAddress = "127.0.0.1"
        )

        val response = handler.handle(request)
        assertEquals(201, response.status)
        assertEquals("application/json", response.contentType)

        @Serializable
        data class CreateOrgResponse(val id: String, val name: String, val created: Boolean)

        val result = JsonSupport.fromJson<CreateOrgResponse>(response.body)
        assertEquals("Test Organization", result.name)
        assertEquals(true, result.created)
        assertTrue(result.id.isNotBlank())

        // Test invalid request (empty name)
        val invalidRequest = CreateOrgRequest("")
        val invalidRequestBody = JsonSupport.toJson(invalidRequest)

        val invalidReq = Request(
            method = "POST",
            uri = "/api/organizations",
            path = "/api/organizations",
            queryParams = emptyMap(),
            pathParams = emptyMap(),
            headers = mapOf("Content-Type" to "application/json"),
            body = invalidRequestBody,
            remoteAddress = "127.0.0.1"
        )

        val invalidResponse = handler.handle(invalidReq)
        assertEquals(400, invalidResponse.status)
        assertTrue(invalidResponse.body.contains("Name is required"))
    }

    @Test
    fun `should apply CORS filter like ProjectVista`() {
        val routes = buildRouter {
            filter(Filters.cors(
                allowedOrigins = listOf("http://localhost:3000"),
                allowCredentials = true
            ))

            get("/api/test") {
                HttpResponse.ok("Test response")
            }

            options("/api/test") {
                HttpResponse.ok("Options response")
            }
        }

        val handler = EnhancedFrameworkHandler(routes)

        // Test regular request with CORS headers
        val request = Request(
            method = "GET",
            uri = "/api/test",
            path = "/api/test",
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

        // Test preflight OPTIONS request
        val preflightRequest = Request(
            method = "OPTIONS",
            uri = "/api/test",
            path = "/api/test",
            queryParams = emptyMap(),
            pathParams = emptyMap(),
            headers = mapOf("Origin" to "http://localhost:3000"),
            body = "",
            remoteAddress = "127.0.0.1"
        )

        val preflightResponse = handler.handle(preflightRequest)
        assertEquals(200, preflightResponse.status)
        assertTrue(preflightResponse.headers.containsKey("Access-Control-Allow-Methods"))
        assertTrue(preflightResponse.headers.containsKey("Access-Control-Allow-Headers"))
    }
}