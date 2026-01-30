package com.consideredweb

import com.consideredweb.core.*
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

@Serializable
data class TestUser(val id: Int, val name: String, val email: String)

@Serializable
data class CreateUserRequest(val name: String, val email: String)

class JsonTest {

    @Test
    fun `should serialize and deserialize JSON`() {
        val user = TestUser(1, "John Doe", "john@example.com")

        // Test serialization
        val json = JsonSupport.toJson(user)
        println("Serialized: $json")

        // Test deserialization
        val deserialized = JsonSupport.fromJson<TestUser>(json)
        assertEquals(user, deserialized)
    }

    @Test
    fun `should create JSON responses`() {
        val user = TestUser(1, "John Doe", "john@example.com")

        // Test JSON response creation
        val response = HttpResponse.okJson(user)
        assertEquals(200, response.status)
        assertEquals("application/json", response.contentType)

        // Verify the body contains valid JSON
        val deserialized = JsonSupport.fromJson<TestUser>(response.body)
        assertEquals(user, deserialized)
    }

    @Test
    fun `should parse request body as JSON`() {
        val createRequest = CreateUserRequest("Jane Doe", "jane@example.com")
        val json = JsonSupport.toJson(createRequest)

        val request = Request(
            method = "POST",
            uri = "/users",
            path = "/users",
            queryParams = emptyMap(),
            pathParams = emptyMap(),
            headers = mapOf("Content-Type" to "application/json"),
            body = json,
            remoteAddress = "127.0.0.1"
        )

        // Test JSON parsing
        val parsed = request.bodyAsJson<CreateUserRequest>()
        assertEquals(createRequest, parsed)
    }

    @Test
    fun `should handle JSON errors gracefully`() {
        val request = Request(
            method = "POST",
            uri = "/users",
            path = "/users",
            queryParams = emptyMap(),
            pathParams = emptyMap(),
            headers = mapOf("Content-Type" to "application/json"),
            body = "invalid json",
            remoteAddress = "127.0.0.1"
        )

        // Test graceful error handling
        val parsed = request.bodyAsJsonOrNull<CreateUserRequest>()
        assertEquals(null, parsed)
    }
}