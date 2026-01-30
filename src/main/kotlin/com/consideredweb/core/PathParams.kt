package com.consideredweb.core

import java.util.*

/**
 * Type-safe path parameter extraction utilities.
 *
 * Provides extensions to safely extract and validate path parameters
 * from requests with proper error handling.
 */

/**
 * Exception thrown when path parameter extraction fails
 */
class PathParamException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Extension functions for Request to support typed path parameter extraction
 */

/**
 * Extract path parameter as String (required)
 */
fun Request.pathParam(name: String): String {
    return pathParams[name] ?: throw PathParamException("Required path parameter '$name' not found")
}

/**
 * Extract path parameter as String (optional)
 */
fun Request.pathParamOrNull(name: String): String? {
    return pathParams[name]
}

/**
 * Extract path parameter as UUID
 */
fun Request.pathParamUuid(name: String): UUID {
    val value = pathParam(name)
    return try {
        UUID.fromString(value)
    } catch (e: IllegalArgumentException) {
        throw PathParamException("Path parameter '$name' is not a valid UUID: $value", e)
    }
}

/**
 * Extract path parameter as UUID (optional)
 */
fun Request.pathParamUuidOrNull(name: String): UUID? {
    val value = pathParamOrNull(name) ?: return null
    return try {
        UUID.fromString(value)
    } catch (e: IllegalArgumentException) {
        null // Return null for invalid UUIDs when using OrNull version
    }
}

/**
 * Extract path parameter as Int
 */
fun Request.pathParamInt(name: String): Int {
    val value = pathParams[name] ?: throw PathParamException("Required path parameter '$name' not found")
    return try {
        value.toInt()
    } catch (e: NumberFormatException) {
        throw PathParamException("Path parameter '$name' is not a valid integer: $value", e)
    }
}

/**
 * Extract path parameter as Int (optional)
 */
fun Request.pathParamIntOrNull(name: String): Int? {
    val value = pathParamOrNull(name) ?: return null
    return try {
        value.toInt()
    } catch (e: NumberFormatException) {
        null
    }
}

/**
 * Extract path parameter as Long
 */
fun Request.pathParamLong(name: String): Long {
    val value = pathParams[name] ?: throw PathParamException("Required path parameter '$name' not found")
    return try {
        value.toLong()
    } catch (e: NumberFormatException) {
        throw PathParamException("Path parameter '$name' is not a valid long: $value", e)
    }
}

/**
 * Extract path parameter as Long (optional)
 */
fun Request.pathParamLongOrNull(name: String): Long? {
    val value = pathParamOrNull(name) ?: return null
    return try {
        value.toLong()
    } catch (e: NumberFormatException) {
        null
    }
}

/**
 * Query parameter extension functions (similar pattern)
 */

/**
 * Extract query parameter as String (required)
 */
fun Request.queryParam(name: String): String {
    return queryParams[name] ?: throw PathParamException("Required query parameter '$name' not found")
}

/**
 * Extract query parameter as String (optional)
 */
fun Request.queryParamOrNull(name: String): String? {
    return queryParams[name]
}

/**
 * Extract query parameter with default value
 */
fun Request.queryParam(name: String, default: String): String {
    return queryParams[name] ?: default
}

/**
 * Extract query parameter as Int
 */
fun Request.queryParamInt(name: String): Int {
    val value = queryParams[name] ?: throw PathParamException("Required query parameter '$name' not found")
    return try {
        value.toInt()
    } catch (e: NumberFormatException) {
        throw PathParamException("Query parameter '$name' is not a valid integer: $value", e)
    }
}

/**
 * Extract query parameter as Int with default
 */
fun Request.queryParamInt(name: String, default: Int): Int {
    val value = queryParamOrNull(name) ?: return default
    return try {
        value.toInt()
    } catch (e: NumberFormatException) {
        throw PathParamException("Query parameter '$name' is not a valid integer: $value", e)
    }
}