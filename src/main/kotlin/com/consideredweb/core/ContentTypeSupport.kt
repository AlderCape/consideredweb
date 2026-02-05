package com.consideredweb.core

import com.consideredweb.utils.traced
import org.slf4j.LoggerFactory

@PublishedApi
internal val contentTypeLogger = LoggerFactory.getLogger(ContentTypeSupport::class.java)

/**
 * Content-type aware deserialization support.
 * Handles different content types automatically based on Content-Type header.
 */
object ContentTypeSupport {

    /**
     * Deserialize request body based on Content-Type header
     */
    inline fun <reified T> Request.bodyAs(): T = traced {
        contentTypeLogger.debug("Converting body from {} format", contentType)
        when (contentType) {
            "application/json" -> bodyAsJson<T>()
            "application/x-www-form-urlencoded" -> bodyAsForm<T>()
            "text/plain" -> bodyAsText<T>()
            null -> bodyAsJson<T>() // Default to JSON if no content type
            else -> throw UnsupportedContentTypeException("Unsupported content type: $contentType")
        }
    }

    /**
     * Deserialize form-encoded data (basic implementation)
     */
    inline fun <reified T> Request.bodyAsForm(): T {
        // For now, throw an exception as form handling requires more complex mapping
        throw UnsupportedContentTypeException("Form-encoded content type not yet implemented")
    }

    /**
     * Handle plain text content
     */
    inline fun <reified T> Request.bodyAsText(): T {
        // Simple string handling - only works if T is String
        if (T::class == String::class) {
            @Suppress("UNCHECKED_CAST")
            return body as T
        }
        throw UnsupportedContentTypeException("Cannot deserialize plain text to ${T::class.simpleName}")
    }
}

class UnsupportedContentTypeException(message: String) : Exception(message)