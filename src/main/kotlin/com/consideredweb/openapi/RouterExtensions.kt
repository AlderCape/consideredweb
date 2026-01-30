package com.consideredweb.openapi

import com.consideredweb.core.Route
import com.consideredweb.core.RouterBuilder
import com.consideredweb.core.buildRouter

/**
 * Extension function to build routes and automatically generate OpenAPI spec file
 */
fun buildRouterWithOpenApi(
    title: String = "API",
    version: String = "1.0.0",
    config: OpenApiConfig? = null,
    block: RouterBuilder.() -> Unit
): List<Route> {
    val routes = buildRouter(block)

    // Generate OpenAPI file
    if (config != null) {
        OpenApiFileGenerator.generateFile(routes, config)
    } else {
        OpenApiFileGenerator.generateFile(routes, title, version)
    }

    return routes
}

/**
 * Extension function for existing RouterBuilder to generate OpenAPI file
 */
fun List<Route>.generateOpenApiFile(
    title: String = "API",
    version: String = "1.0.0",
    config: OpenApiConfig? = null
): List<Route> {
    if (config != null) {
        OpenApiFileGenerator.generateFile(this, config)
    } else {
        OpenApiFileGenerator.generateFile(this, title, version)
    }
    return this
}

/**
 * Convenience function to clear typed route registry between tests
 */
fun clearTypedRouteRegistry() {
    OpenApiGenerator.clearRegistry()
}