package com.consideredweb

import com.consideredweb.core.*
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for typed routes with automatic JSON serialization/deserialization.
 */
class TypedRoutesTest {

    @Serializable
    data class TestRequest(val name: String, val value: Int)

    @Serializable
    data class TestResponse(val message: String, val input: TestRequest)

    @Test
    fun `should handle postJson with typed handler`() {
        val routes = buildRouter {
            post<TestRequest, TestResponse>("/test") { request ->
                TestResponse("Received: ${request.name}", request)
            }
        }

        val handler = EnhancedFrameworkHandler(routes)

        val requestBody = JsonSupport.toJson(TestRequest("test", 42))
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
        assertEquals(201, response.status) // Created status for POST
        assertEquals("application/json", response.contentType)

        val result = JsonSupport.fromJson<TestResponse>(response.body)
        assertEquals("Received: test", result.message)
        assertEquals("test", result.input.name)
        assertEquals(42, result.input.value)
    }

    @Test
    fun `should handle getJson with no parameters`() {
        val routes = buildRouter {
            get<List<String>>("/items") {
                listOf("item1", "item2", "item3")
            }
        }

        val handler = EnhancedFrameworkHandler(routes)

        val request = Request(
            method = "GET",
            uri = "/items",
            path = "/items",
            queryParams = emptyMap(),
            pathParams = emptyMap(),
            headers = emptyMap(),
            body = "",
            remoteAddress = "127.0.0.1"
        )

        val response = handler.handle(request)
        assertEquals(200, response.status)
        assertEquals("application/json", response.contentType)

        val result = JsonSupport.fromJson<List<String>>(response.body)
        assertEquals(3, result.size)
        assertEquals("item1", result[0])
    }

    @Test
    fun `should handle patchJson with request access`() {
        val routes = buildRouter {
            patch<TestRequest, TestResponse>("/test/{id}") { body, request ->
                val id = request.pathParamUuid("id")
                TestResponse("Updated ${body.name} with ID $id", body)
            }
        }

        val handler = EnhancedFrameworkHandler(routes)

        val testId = java.util.UUID.randomUUID()
        val requestBody = JsonSupport.toJson(TestRequest("updated", 100))
        val request = Request(
            method = "PATCH",
            uri = "/test/$testId",
            path = "/test/$testId",
            queryParams = emptyMap(),
            pathParams = mapOf("id" to testId.toString()),
            headers = mapOf("Content-Type" to "application/json"),
            body = requestBody,
            remoteAddress = "127.0.0.1"
        )

        val response = handler.handle(request)
        assertEquals(200, response.status) // OK status for PATCH
        assertEquals("application/json", response.contentType)

        val result = JsonSupport.fromJson<TestResponse>(response.body)
        assertTrue(result.message.contains("Updated updated"))
        assertTrue(result.message.contains(testId.toString()))
    }

    @Test
    fun `should handle invalid JSON gracefully`() {
        val routes = buildRouter {
            post<TestRequest, TestResponse>("/test") { request ->
                TestResponse("Should not reach here", request)
            }
        }

        val handler = EnhancedFrameworkHandler(routes)

        val request = Request(
            method = "POST",
            uri = "/test",
            path = "/test",
            queryParams = emptyMap(),
            pathParams = emptyMap(),
            headers = mapOf("Content-Type" to "application/json"),
            body = "invalid json",
            remoteAddress = "127.0.0.1"
        )

        val response = handler.handle(request)
        assertEquals(400, response.status) // Bad Request
        assertTrue(response.body.contains("Invalid request"))
    }
}