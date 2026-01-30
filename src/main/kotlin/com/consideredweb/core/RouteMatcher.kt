package com.consideredweb.core

import com.consideredweb.utils.traced

class RouteMatcher(private val routes: List<Route>) {

    fun match(method: HttpMethod, path: String): MatchResult? = traced {
        for (route in routes) {
            if (route.method == method) {
                val pathParams = matchPath(route.path, path)
                if (pathParams != null) {
                    return@traced MatchResult(route, pathParams)
                }
            }
        }
        null
    }

    private fun matchPath(routePath: String, requestPath: String): Map<String, String>? {
        val routeSegments = routePath.split("/")
        val requestSegments = requestPath.split("/")

        if (routeSegments.size != requestSegments.size) {
            return null
        }

        val pathParams = mutableMapOf<String, String>()

        for (i in routeSegments.indices) {
            val routeSegment = routeSegments[i]
            val requestSegment = requestSegments[i]

            when {
                routeSegment.startsWith("{") && routeSegment.endsWith("}") -> {
                    // Path parameter
                    val paramName = routeSegment.substring(1, routeSegment.length - 1)
                    pathParams[paramName] = requestSegment
                }
                routeSegment == requestSegment -> {
                    // Exact match
                    continue
                }
                else -> {
                    // No match
                    return null
                }
            }
        }

        return pathParams
    }
}

data class MatchResult(
    val route: Route,
    val pathParams: Map<String, String>
)