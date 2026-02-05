package com.consideredweb.core

import com.consideredweb.utils.traced
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(FrameworkHandler::class.java)

class FrameworkHandler(routes: List<Route>) : HttpHandler {
    private val routeMatcher = RouteMatcher(routes)

    override fun handle(request: Request): HttpResponse = traced {
        val method = HttpMethod.valueOf(request.method.uppercase())
        // Strip query parameters from path for route matching
        val pathWithoutQuery = request.path.split("?")[0]
        val matchResult = routeMatcher.match(method, pathWithoutQuery)

        logger.debug("Route match for {} {}: {}", method, pathWithoutQuery, matchResult != null)
        if (matchResult != null) {
            // Create request with path parameters
            val requestWithParams = request.copy(pathParams = matchResult.pathParams)

            try {
                matchResult.route.handler(requestWithParams).also {
                    logger.debug("Response status: {}", it.status)
                }
            } catch (e: Exception) {
                logger.error("Error handling route {} {}: {}", method, pathWithoutQuery, e.message, e)
                val errorJson = """{"message": "Internal server error: ${e.message}", "code": "INTERNAL_ERROR"}"""
                HttpResponse.Companion.internalServerError(errorJson)
                    .withHeader("Content-Type", "application/json")
            }
        } else {
            logger.debug("No route found for {} {}", method, pathWithoutQuery)
            val errorJson = """{"message": "Route not found: ${request.method} $pathWithoutQuery", "code": "ROUTE_NOT_FOUND"}"""
            HttpResponse.Companion.notFound(errorJson)
                .withHeader("Content-Type", "application/json")
        }
    }
}