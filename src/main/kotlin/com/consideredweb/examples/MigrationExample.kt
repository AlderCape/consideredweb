package com.consideredweb.examples

import com.consideredweb.core.*
import com.consideredweb.server.JavaHttpServer
import kotlinx.serialization.Serializable

/**
 * Example showing how to migrate ProjectVista routes from http4k to ConsideredWeb.
 *
 * This demonstrates migrating the health check endpoints from ProjectVista's Routes.kt
 * to use the ConsideredWeb framework with the same functionality.
 */

// Data classes for health check responses
@Serializable
data class HealthReport(
    val status: String,
    val checks: Map<String, String>
)

// Simple mock health service to demonstrate the pattern
class MockHealthService {
    fun check(): HealthReport {
        return HealthReport(
            status = "healthy",
            checks = mapOf(
                "database" to "connected",
                "blobStorage" to "connected"
            )
        )
    }
}

fun main() {
    val healthService = MockHealthService()

    val routes = buildRouterAndPrint {
        // Add CORS support like ProjectVista
        filter(Filters.cors(
            allowedOrigins = listOf("http://localhost:3000", "https://*.projectvista.com"),
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"),
            allowedHeaders = listOf("content-type", "authorization"),
            allowCredentials = true
        ))

        // Add error handling
        filter(Filters.errorHandling())

        // Basic health check - equivalent to ProjectVista's /health endpoint
        get("/health") {
            HttpResponse.ok("OK")
        }

        // Deep health check - equivalent to ProjectVista's /health/deep endpoint
        get("/health/deep") { request ->
            // Optional token guard for deep health (like ProjectVista)
            val token = System.getenv("HEALTHCHECK_TOKEN")
            val allowed = token.isNullOrBlank() || request.header("X-Health-Token") == token

            if (!allowed) {
                HttpResponse.forbidden("Forbidden")
            } else {
                val report = healthService.check()
                HttpResponse.okJson(report)
            }
        }

        // Example API endpoint showing JSON handling
        get("/api/status") {
            @Serializable
            data class StatusResponse(val service: String, val version: String, val framework: String)

            HttpResponse.okJson(StatusResponse(
                service = "ProjectVista ConsideredWeb Migration",
                version = "1.0.0",
                framework = "ConsideredWeb"
            ))
        }

        // Example with path parameter (like ProjectVista's user endpoints)
        get("/api/organizations/{id}") { request ->
            @Serializable
            data class OrganizationResponse(val id: String, val name: String, val type: String)

            val id = request.pathParamUuid("id")
            HttpResponse.okJson(OrganizationResponse(
                id = id.toString(),
                name = "Sample Organization",
                type = "Construction Company"
            ))
        }

        // Example POST endpoint showing request body parsing
        post("/api/organizations") { request ->
            @Serializable
            data class CreateOrgRequest(val name: String)

            @Serializable
            data class CreateOrgResponse(val id: String, val name: String, val created: Boolean)

            try {
                // Parse JSON request body (like ProjectVista's create endpoints)
                val body = request.bodyAsJson<CreateOrgRequest>()
                if (body.name.isBlank()) {
                    throw IllegalArgumentException("Name is required")
                }

                // Simulate creating organization and returning response
                HttpResponse.createdJson(CreateOrgResponse(
                    id = java.util.UUID.randomUUID().toString(),
                    name = body.name,
                    created = true
                ))
            } catch (e: Exception) {
                HttpResponse.badRequest("Invalid request: ${e.message}")
            }
        }
    }

    val handler = EnhancedFrameworkHandler(routes)
    val server = JavaHttpServer()

    server.start(8080, handler)

    println("ProjectVista Migration Example running on port 8080")
    println("Migration Examples:")
    println("  GET  /health                    - Basic health check (migrated from ProjectVista)")
    println("  GET  /health/deep               - Deep health check with optional token")
    println("  GET  /api/status                - JSON status endpoint")
    println("  GET  /api/organizations/{uuid}  - Path parameter example")
    println("  POST /api/organizations         - JSON request body example")
    println()
    println("Test with:")
    println("  curl http://localhost:8080/health")
    println("  curl http://localhost:8080/health/deep")
    println("  curl -H 'X-Health-Token: secret' http://localhost:8080/health/deep")
    println("  curl http://localhost:8080/api/organizations/$(uuidgen)")
    println("  curl -X POST -H 'Content-Type: application/json' -d '{\"name\":\"Test Org\"}' http://localhost:8080/api/organizations")
    println()
    println("Press Ctrl+C to stop")

    // Keep the server running
    Runtime.getRuntime().addShutdownHook(Thread {
        server.stop()
    })

    Thread.currentThread().join()
}