package com.consideredweb.core

typealias RouteHandler = (Request) -> HttpResponse

data class Route(
    val method: HttpMethod,
    val path: String,
    val handler: RouteHandler
)

class RouteBuilder {
    private val routes = mutableListOf<Route>()

    fun route(method: HttpMethod, path: String, handler: RouteHandler) {
        routes.add(Route(method, path, handler))
    }

    fun get(path: String, handler: RouteHandler) =
        route(HttpMethod.GET, path, handler)

    fun post(path: String, handler: RouteHandler) =
        route(HttpMethod.POST, path, handler)

    fun put(path: String, handler: RouteHandler) =
        route(HttpMethod.PUT, path, handler)

    fun patch(path: String, handler: RouteHandler) =
        route(HttpMethod.PATCH, path, handler)

    fun delete(path: String, handler: RouteHandler) =
        route(HttpMethod.DELETE, path, handler)

    fun head(path: String, handler: RouteHandler) =
        route(HttpMethod.HEAD, path, handler)

    fun options(path: String, handler: RouteHandler) =
        route(HttpMethod.OPTIONS, path, handler)

    fun build(): List<Route> = routes.toList()
}

fun buildRoutes(block: RouteBuilder.() -> Unit): List<Route> {
    val builder = RouteBuilder()
    builder.block()
    return builder.build()
}