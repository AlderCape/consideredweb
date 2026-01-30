package com.consideredweb.auth

import com.consideredweb.core.Filter
import com.consideredweb.core.HttpHandler
import com.consideredweb.core.HttpResponse
import com.consideredweb.core.Request
import com.consideredweb.core.withHeader
import java.time.Instant

/**
 * Generic JWT authentication support for ConsideredWeb.
 *
 * Applications can implement their own JWT validation logic and claims structure
 * while the framework handles the HTTP integration.
 */

/**
 * Generic interface for JWT claims that applications can implement
 */
interface JwtClaims {
    val subject: String
    val issuedAt: Instant
    val expiresAt: Instant
}

/**
 * Interface for applications to provide their JWT validation logic
 */
interface JwtValidator<T : JwtClaims> {
    /**
     * Validate a JWT token and extract claims
     * @param token The JWT token string (without "Bearer " prefix)
     * @return Claims if valid, null if invalid
     */
    fun validateToken(token: String): T?
}

/**
 * Interface for applications to provide fallback authentication (e.g., header-based for testing)
 */
interface FallbackAuthProvider<T : JwtClaims> {
    /**
     * Extract authentication info from request headers when no JWT is present
     * @param request The HTTP request
     * @return Claims if authentication headers are valid, null otherwise
     */
    fun extractFromHeaders(request: Request): T?
}

/**
 * Interface for enriching requests with authentication context
 */
interface RequestEnricher<T : JwtClaims> {
    /**
     * Enrich a request with authentication context from validated claims
     * @param request The original request
     * @param claims The validated JWT claims
     * @return The enriched request
     */
    fun enrichRequest(request: Request, claims: T): Request
}

/**
 * JWT Authentication Filter for ConsideredWeb
 *
 * @param jwtValidator Application-specific JWT validation logic
 * @param requestEnricher Application-specific request enrichment logic
 * @param fallbackAuthProvider Optional fallback authentication for testing/development
 */
class JwtAuthFilter<T : JwtClaims>(
    private val jwtValidator: JwtValidator<T>,
    private val requestEnricher: RequestEnricher<T>,
    private val fallbackAuthProvider: FallbackAuthProvider<T>? = null
) {

    fun create(): Filter = Filter { handler ->
        HttpHandler { request ->
            val authHeader = request.header("Authorization")

            // Try JWT authentication first
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                val token = authHeader.substring(7) // Remove "Bearer " prefix

                val claims = jwtValidator.validateToken(token)
                if (claims != null) {
                    // Valid JWT - enrich request and continue
                    val enrichedRequest = requestEnricher.enrichRequest(request, claims)
                    handler.handle(enrichedRequest)
                } else {
                    // Invalid JWT
                    val errorJson = """{"message": "Invalid or expired token", "code": "UNAUTHORIZED"}"""
                    HttpResponse(401, errorJson, "application/json")
                        .withHeader("Content-Type", "application/json")
                }
            } else {
                // No JWT - try fallback authentication if available
                if (fallbackAuthProvider != null) {
                    val claims = fallbackAuthProvider.extractFromHeaders(request)
                    if (claims != null) {
                        // Valid fallback auth - enrich request and continue
                        val enrichedRequest = requestEnricher.enrichRequest(request, claims)
                        handler.handle(enrichedRequest)
                    } else {
                        // Invalid fallback auth
                        val errorJson = """{"message": "Missing or invalid authentication", "code": "UNAUTHORIZED"}"""
                        HttpResponse(401, errorJson, "application/json")
                            .withHeader("Content-Type", "application/json")
                    }
                } else {
                    // No fallback - require JWT
                    val errorJson = """{"message": "Missing Authorization header", "code": "UNAUTHORIZED"}"""
                    HttpResponse(401, errorJson, "application/json")
                        .withHeader("Content-Type", "application/json")
                }
            }
        }
    }
}