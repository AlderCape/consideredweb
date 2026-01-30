package com.consideredweb.core

import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * Metadata about typed routes for OpenAPI generation
 */
data class TypedRouteMetadata(
    val method: HttpMethod,
    val path: String,
    val requestType: KClass<*>? = null,
    val responseType: KClass<*>? = null,
    val requestKType: KType? = null,
    val responseKType: KType? = null
)

/**
 * Global registry for typed route metadata
 * This is populated during route building and used for OpenAPI generation
 */
object TypedRouteRegistry {
    private val typedRoutes = mutableListOf<TypedRouteMetadata>()

    fun addRoute(metadata: TypedRouteMetadata) {
        typedRoutes.add(metadata)
    }

    fun getAllRoutes(): List<TypedRouteMetadata> = typedRoutes.toList()

    fun clear() {
        typedRoutes.clear()
    }

    fun getRoutesForPath(path: String): List<TypedRouteMetadata> {
        return typedRoutes.filter { it.path == path }
    }
}

/**
 * Capture typed route metadata for OpenAPI generation
 */
fun captureTypedRouteMetadata(
    method: HttpMethod,
    path: String,
    requestType: KClass<*>? = null,
    responseType: KClass<*>? = null
) {
    val metadata = TypedRouteMetadata(method, path, requestType, responseType)
    TypedRouteRegistry.addRoute(metadata)
}

/**
 * Capture typed route metadata with full KType information for OpenAPI generation
 */
fun captureTypedRouteMetadata(
    method: HttpMethod,
    path: String,
    requestType: KClass<*>? = null,
    responseType: KClass<*>? = null,
    requestKType: KType? = null,
    responseKType: KType? = null
) {
    val metadata = TypedRouteMetadata(method, path, requestType, responseType, requestKType, responseKType)
    TypedRouteRegistry.addRoute(metadata)
}