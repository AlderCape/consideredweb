package com.consideredweb.openapi

import com.consideredweb.core.Route
import kotlin.reflect.KType

/**
 * Analyzes routes to extract type information for OpenAPI generation
 */
object RouteAnalyzer {

    data class RouteInfo(
        val route: Route,
        val requestBodyType: KType? = null,
        val responseType: KType? = null,
        val pathParameters: List<String> = emptyList()
    )

    /**
     * Extract path parameters from a route path (e.g., "/users/{id}" -> ["id"])
     */
    fun extractPathParameters(path: String): List<String> {
        val regex = "\\{([^}]+)}".toRegex()
        return regex.findAll(path).map { it.groupValues[1] }.toList()
    }
}