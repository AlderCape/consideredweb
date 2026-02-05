package com.consideredweb.core

import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.consideredweb.core.RouterBuilder")

/**
 * Enhanced router builder with filter support.
 *
 * Provides a more powerful routing DSL that supports middleware,
 * filter chains, and nested route groups.
 */

class RouterBuilder {
    private val routes = mutableListOf<Route>()
    private val filters = mutableListOf<Filter>()

    /**
     * Add a filter to apply to all routes in this builder
     */
    fun filter(filter: Filter) {
        filters.add(filter)
    }

    /**
     * Add multiple filters
     */
    fun filters(vararg newFilters: Filter) {
        filters.addAll(newFilters)
    }

    /**
     * Create a route with the current filter chain applied
     */
    fun route(method: HttpMethod, path: String, handler: RouteHandler) {
        val finalHandler = applyFilters(handler)
        routes.add(Route(method, path, finalHandler))
    }

    /**
     * HTTP method convenience functions
     */
    fun get(path: String, handler: RouteHandler) = route(HttpMethod.GET, path, handler)
    fun post(path: String, handler: RouteHandler) = route(HttpMethod.POST, path, handler)
    fun put(path: String, handler: RouteHandler) = route(HttpMethod.PUT, path, handler)
    fun patch(path: String, handler: RouteHandler) = route(HttpMethod.PATCH, path, handler)
    fun delete(path: String, handler: RouteHandler) = route(HttpMethod.DELETE, path, handler)
    fun head(path: String, handler: RouteHandler) = route(HttpMethod.HEAD, path, handler)
    fun options(path: String, handler: RouteHandler) = route(HttpMethod.OPTIONS, path, handler)


    /**
     * Create a nested route group with additional filters
     */
    fun group(pathPrefix: String = "", block: RouterBuilder.() -> Unit) {
        val groupBuilder = RouterBuilder()
        groupBuilder.filters.addAll(this.filters) // Inherit parent filters
        groupBuilder.block()

        // Add grouped routes with path prefix
        for (route in groupBuilder.build()) {
            val fullPath = if (pathPrefix.isNotEmpty()) {
                val cleanPrefix = pathPrefix.removeSuffix("/")
                val cleanRoutePath = route.path.removePrefix("/")
                if (cleanRoutePath.isEmpty()) {
                    cleanPrefix
                } else {
                    "$cleanPrefix/$cleanRoutePath"
                }
            } else {
                route.path
            }
            routes.add(route.copy(path = fullPath))
        }
    }

    /**
     * Create a nested route group with additional filters
     */
    fun group(pathPrefix: String = "", filter: Filter, block: RouterBuilder.() -> Unit) {
        val groupBuilder = RouterBuilder()
        groupBuilder.filters.addAll(this.filters) // Inherit parent filters
        groupBuilder.filter(filter) // Add group-specific filter
        groupBuilder.block()

        // Add grouped routes with path prefix
        for (route in groupBuilder.build()) {
            val fullPath = if (pathPrefix.isNotEmpty()) {
                val cleanPrefix = pathPrefix.removeSuffix("/")
                val cleanRoutePath = route.path.removePrefix("/")
                if (cleanRoutePath.isEmpty()) {
                    cleanPrefix
                } else {
                    "$cleanPrefix/$cleanRoutePath"
                }
            } else {
                route.path
            }
            routes.add(route.copy(path = fullPath))
        }
    }

    /**
     * Apply all filters to a handler in order
     */
    private fun applyFilters(handler: RouteHandler): RouteHandler {
        val httpHandler = HttpHandler { request -> handler(request) }
        val filteredHandler = filters.fold(httpHandler) { h, filter -> filter.filter(h) }
        return { request -> filteredHandler.handle(request) }
    }

    fun build(): List<Route> = routes.toList()

    /**
     * Print all routes in a readable format
     */
    fun printRoutes() {
        val routeList = build()
        if (routeList.isEmpty()) {
            println("No routes defined")
            return
        }

        println("📋 Route List:")
        println("=".repeat(50))

        routeList
            .sortedWith(compareBy<Route> { it.path }.thenBy { it.method.name })
            .forEach { route ->
                val method = route.method.name.padEnd(7)
                val path = route.path
                println("  $method $path")
            }

        println("=".repeat(50))
        println("Total: ${routeList.size} routes")
    }
}

/**
 * DSL function to build routes with filter support
 */
fun buildRouter(block: RouterBuilder.() -> Unit): List<Route> {
    val builder = RouterBuilder()
    builder.block()
    return builder.build()
}

/**
 * DSL function to build routes and print them for debugging
 */
fun buildRouterAndPrint(block: RouterBuilder.() -> Unit): List<Route> {
    val builder = RouterBuilder()
    builder.block()
    builder.printRoutes()
    return builder.build()
}

/**
 * Enhanced FrameworkHandler that supports method override and better error handling
 */
class EnhancedFrameworkHandler(routes: List<Route>) : HttpHandler {
    private val frameworkHandler = FrameworkHandler(routes)

    override fun handle(request: Request): HttpResponse {
        return try {
            // Support method override via header (for browsers that only support GET/POST)
            val actualMethod = request.header("X-HTTP-Method-Override") ?: request.method
            val enhancedRequest = if (actualMethod != request.method) {
                request.copy(method = actualMethod)
            } else {
                request
            }
            logger.debug("Handling request: {} {}", enhancedRequest.method, enhancedRequest.path)

            frameworkHandler.handle(enhancedRequest)
        } catch (e: PathParamException) {
            HttpResponse.badRequest("Invalid parameter: ${e.message}")
        } catch (e: Exception) {
            logger.error("Error handling request: {}", e.message, e)
            HttpResponse.internalServerError("Internal server error")
        }
    }
}