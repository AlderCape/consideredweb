package com.consideredweb.core

import com.consideredweb.utils.traced
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory

@PublishedApi
internal val jsonLogger = LoggerFactory.getLogger(JsonSupport::class.java)

/**
 * JSON serialization utilities for ConsideredWeb.
 *
 * Provides simple, type-safe JSON serialization and deserialization
 * using kotlinx.serialization.
 */
object JsonSupport {

    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    val prettyJson = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
        explicitNulls = false
    }

    /**
     * Serialize an object to JSON string
     */
    inline fun <reified T> toJson(value: T): String {
        return json.encodeToString(value)
    }

    /**
     * Serialize an object to pretty-printed JSON string
     */
    inline fun <reified T> toPrettyJson(value: T): String {
        return prettyJson.encodeToString(value)
    }

    /**
     * Deserialize JSON string to object
     */
    inline fun <reified T> fromJson(jsonString: String): T {
        jsonLogger.debug("Deserializing JSON: {}", jsonString.take(200))
        return json.decodeFromString(jsonString)
    }
}

/**
 * Extension functions for HttpResponse to support JSON serialization
 */
inline fun <reified T> HttpResponse.Companion.json(value: T, status: Int = 200): HttpResponse {
    return HttpResponse(status, JsonSupport.toJson(value), "application/json")
}

inline fun <reified T> HttpResponse.Companion.okJson(value: T): HttpResponse {
    return json(value, 200)
}

inline fun <reified T> HttpResponse.Companion.createdJson(value: T): HttpResponse {
    return json(value, 201)
}

/**
 * Extension functions for Request to support JSON deserialization
 */
inline fun <reified T> Request.bodyAsJson(): T = traced {
    jsonLogger.debug("Converting body to JSON, length: {}", this.body.length)
    JsonSupport.fromJson(this.body)
}

inline fun <reified T> Request.bodyAsJsonOrNull(): T? {
    return try {
        jsonLogger.debug("Converting body to JSON or null, length: {}", body.length)
        if (body.isBlank()) null else JsonSupport.fromJson<T>(body)
    } catch (e: Exception) {
        jsonLogger.debug("Failed to parse JSON: {}", e.message)
        null
    }
}