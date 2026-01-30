package com.consideredweb.core

import com.consideredweb.utils.traced

class FrameworkHandler(routes: List<Route>) : HttpHandler {
    private val routeMatcher = RouteMatcher(routes)

    override fun handle(request: Request): HttpResponse = traced {
        val method = HttpMethod.valueOf(request.method.uppercase())
        // Strip query parameters from path for route matching
        val pathWithoutQuery = request.path.split("?")[0]
        val matchResult = routeMatcher.match(method, pathWithoutQuery)

        println("Have match result: $matchResult")
        if (matchResult != null) {
            // Create request with path parameters
            val requestWithParams = request.copy(pathParams = matchResult.pathParams)

            try {
                matchResult.route.handler(requestWithParams).also {
                    println("Match result: $it")
                }
            } catch (e: Exception) {
                // Basic exception handling - can be enhanced later
                e.printStackTrace()
                val errorJson = """{"message": "Internal server error: ${e.message}", "code": "INTERNAL_ERROR"}"""
                HttpResponse.Companion.internalServerError(errorJson)
                    .withHeader("Content-Type", "application/json")
            }
        } else {
            val errorJson = """{"message": "Route not found: ${request.method} $pathWithoutQuery", "code": "ROUTE_NOT_FOUND"}"""
            HttpResponse.Companion.notFound(errorJson)
                .withHeader("Content-Type", "application/json")
        }
    }
}