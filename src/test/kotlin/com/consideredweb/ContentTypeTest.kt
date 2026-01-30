package com.consideredweb

import com.consideredweb.core.*
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for content-type based automatic deserialization.
 */
class ContentTypeTest {

    @Serializable
    data class TestData(val message: String, val count: Int)

    @Test
    fun `should deserialize JSON based on content type`() {
        val routes = buildRouter {
            post<TestData, String>("/test") { data ->
                "Received: ${data.message} with count ${data.count}"
            }
        }

        val handler = EnhancedFrameworkHandler(routes)

        val requestBody = JsonSupport.toJson(TestData("hello", 42))
        val request = Request(
            method = "POST",
            uri = "/test",
            path = "/test",
            queryParams = emptyMap(),
            pathParams = emptyMap(),
            headers = mapOf("Content-Type" to "application/json"),
            body = requestBody,
            remoteAddress = "127.0.0.1"
        )

        val response = handler.handle(request)
        assertEquals(201, response.status)
        assertEquals("application/json", response.contentType)

        val result = JsonSupport.fromJson<String>(response.body)
        assertEquals("Received: hello with count 42", result)
    }

    @Test
    fun `should default to JSON when no content type specified`() {
        val routes = buildRouter {
            post<TestData, String>("/test") { data ->
                "Received: ${data.message}"
            }
        }

        val handler = EnhancedFrameworkHandler(routes)

        val requestBody = JsonSupport.toJson(TestData("default", 0))
        val request = Request(
            method = "POST",
            uri = "/test",
            path = "/test",
            queryParams = emptyMap(),
            pathParams = emptyMap(),
            headers = emptyMap(), // No Content-Type header
            body = requestBody,
            remoteAddress = "127.0.0.1"
        )

        val response = handler.handle(request)
        assertEquals(201, response.status)

        val result = JsonSupport.fromJson<String>(response.body)
        assertEquals("Received: default", result)
    }

    @Test
    fun `should handle content type with charset parameter`() {
        val routes = buildRouter {
            post<TestData, String>("/test") { data ->
                "Processed: ${data.message}"
            }
        }

        val handler = EnhancedFrameworkHandler(routes)

        val requestBody = JsonSupport.toJson(TestData("charset", 123))
        val request = Request(
            method = "POST",
            uri = "/test",
            path = "/test",
            queryParams = emptyMap(),
            pathParams = emptyMap(),
            headers = mapOf("Content-Type" to "application/json; charset=utf-8"),
            body = requestBody,
            remoteAddress = "127.0.0.1"
        )

        val response = handler.handle(request)
        assertEquals(201, response.status)

        val result = JsonSupport.fromJson<String>(response.body)
        assertEquals("Processed: charset", result)
    }

    @Test
    fun `should reject unsupported content types`() {
        val routes = buildRouter {
            post<TestData, String>("/test") { data ->
                "Should not reach here"
            }
        }

        val handler = EnhancedFrameworkHandler(routes)

        val request = Request(
            method = "POST",
            uri = "/test",
            path = "/test",
            queryParams = emptyMap(),
            pathParams = emptyMap(),
            headers = mapOf("Content-Type" to "application/xml"),
            body = "<test>data</test>",
            remoteAddress = "127.0.0.1"
        )

        val response = handler.handle(request)
        assertEquals(400, response.status)
        assertTrue(response.body.contains("Unsupported content type"))
    }

    @Test
    fun `should handle plain text content type for String type`() {
        val routes = buildRouter {
            post<String, String>("/text") { text ->
                "Echo: $text"
            }
        }

        val handler = EnhancedFrameworkHandler(routes)

        val request = Request(
            method = "POST",
            uri = "/text",
            path = "/text",
            queryParams = emptyMap(),
            pathParams = emptyMap(),
            headers = mapOf("Content-Type" to "text/plain"),
            body = "Hello World",
            remoteAddress = "127.0.0.1"
        )

        val response = handler.handle(request)
        assertEquals(201, response.status)

        val result = JsonSupport.fromJson<String>(response.body)
        assertEquals("Echo: Hello World", result)
    }
}