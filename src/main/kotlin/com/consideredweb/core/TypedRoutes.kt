package com.consideredweb.core

import com.consideredweb.core.ContentTypeSupport.bodyAs
import kotlin.reflect.typeOf

/**
 * Handle exceptions and return appropriate HTTP responses
 */
fun handleException(e: Exception): HttpResponse {
    e.printStackTrace()
    return when (e::class.simpleName) {
        "UnauthorizedException" -> {
            val errorJson = """{"message": "${e.message}", "code": "UNAUTHORIZED"}"""
            HttpResponse(401, errorJson, "application/json")
                .withHeader("Content-Type", "application/json")
        }
        "ForbiddenException" -> {
            val errorJson = """{"message": "${e.message}", "code": "FORBIDDEN"}"""
            HttpResponse(403, errorJson, "application/json")
                .withHeader("Content-Type", "application/json")
        }
        "NotFoundException" -> {
            val errorJson = """{"message": "${e.message}", "code": "NOT_FOUND"}"""
            HttpResponse(404, errorJson, "application/json")
                .withHeader("Content-Type", "application/json")
        }
        else -> {
            val errorJson = """{"message": "Invalid request: ${e.message}", "code": "BAD_REQUEST"}"""
            HttpResponse.badRequest(errorJson)
                .withHeader("Content-Type", "application/json")
        }
    }
}

/**
 * Typed route extensions for automatic content-type based serialization/deserialization.
 *
 * These extensions allow handlers to work with typed objects directly
 * instead of manually parsing request bodies and creating responses.
 * Content type is automatically detected from the Content-Type header.
 */

/**
 * Typed handler that automatically deserializes request body and serializes response
 */
typealias TypedHandler<TRequest, TResponse> = (TRequest) -> TResponse

/**
 * Typed handler with access to the original request for path parameters, headers, etc.
 */
typealias TypedRequestHandler<TRequest, TResponse> = (TRequest, Request) -> TResponse

/**
 * POST route with automatic content-type based body deserialization and response serialization
 */
inline fun <reified TRequest, reified TResponse> RouterBuilder.post(
    path: String,
    crossinline handler: TypedHandler<TRequest, TResponse>
) {
    // Capture type information for OpenAPI generation
    captureTypedRouteMetadata(HttpMethod.POST, path, TRequest::class, TResponse::class)

    post(path) { request ->
        try {
            println("RouterBuilder.post: $request")
            val body = request.bodyAs<TRequest>()
            println("request body: $body")
            val result = handler(body)
            HttpResponse.createdJson(result)
        } catch (e: Exception) {
            handleException(e)
        }
    }
}

/**
 * POST route with access to request and automatic content-type based handling
 */
inline fun <reified TRequest, reified TResponse> RouterBuilder.post(
    path: String,
    crossinline handler: TypedRequestHandler<TRequest, TResponse>
) {
    // Capture type information for OpenAPI generation
    captureTypedRouteMetadata(HttpMethod.POST, path, TRequest::class, TResponse::class)

    post(path) { request ->
        try {
            val body = request.bodyAs<TRequest>()
            val result = handler(body, request)
            HttpResponse.createdJson(result)
        } catch (e: Exception) {
            handleException(e)
        }
    }
}

/**
 * PATCH route with automatic content-type based body deserialization and response serialization
 */
inline fun <reified TRequest, reified TResponse> RouterBuilder.patch(
    path: String,
    crossinline handler: TypedHandler<TRequest, TResponse>
) {
    // Capture type information for OpenAPI generation
    captureTypedRouteMetadata(HttpMethod.PATCH, path, TRequest::class, TResponse::class)

    patch(path) { request ->
        try {
            val body = request.bodyAs<TRequest>()
            val result = handler(body)
            HttpResponse.okJson(result)
        } catch (e: Exception) {
            handleException(e)
        }
    }
}

/**
 * PATCH route with access to request and automatic content-type based handling
 */
inline fun <reified TRequest, reified TResponse> RouterBuilder.patch(
    path: String,
    crossinline handler: TypedRequestHandler<TRequest, TResponse>
) {
    // Capture type information for OpenAPI generation
    captureTypedRouteMetadata(HttpMethod.PATCH, path, TRequest::class, TResponse::class)

    patch(path) { request ->
        try {
            val body = request.bodyAs<TRequest>()
            val result = handler(body, request)
            HttpResponse.okJson(result)
        } catch (e: Exception) {
            handleException(e)
        }
    }
}

/**
 * GET route with automatic JSON response serialization
 */
inline fun <reified TResponse> RouterBuilder.getJson(
    path: String,
    crossinline handler: (Request) -> TResponse
) {
    // Capture type information for OpenAPI generation
    captureTypedRouteMetadata(HttpMethod.GET, path, null, TResponse::class, null, typeOf<TResponse>())

    get(path) { request ->
        try {
            val result = handler(request)
            HttpResponse.okJson(result)
        } catch (e: Exception) {
            handleException(e)
        }
    }
}

/**
 * GET route with no parameters that returns JSON
 */
inline fun <reified TResponse> RouterBuilder.get(
    path: String,
    crossinline handler: () -> TResponse
) {
    // Capture type information for OpenAPI generation
    captureTypedRouteMetadata(HttpMethod.GET, path, null, TResponse::class, null, typeOf<TResponse>())

    get(path) { _ ->
        try {
            val result = handler()
            HttpResponse.okJson(result)
        } catch (e: Exception) {
            handleException(e)
        }
    }
}