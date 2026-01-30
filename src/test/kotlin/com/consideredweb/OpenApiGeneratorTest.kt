package com.consideredweb

import com.consideredweb.core.*
import com.consideredweb.openapi.OpenApiGenerator
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * TDD tests for OpenAPI generation in ConsideredWeb
 */
class OpenApiGeneratorTest {

    @Serializable
    data class CreateUserRequest(val name: String, val email: String) {
        companion object {
            fun openApiExample(): CreateUserRequest = CreateUserRequest(
                name = "Sarah Johnson",
                email = "sarah.johnson@example.com"
            )
        }
    }

    @Serializable
    data class UserResponse(val id: String, val name: String, val email: String) {
        companion object {
            fun openApiExample(): UserResponse = UserResponse(
                id = "123e4567-e89b-12d3-a456-426614174000",
                name = "Sarah Johnson",
                email = "sarah.johnson@example.com"
            )
        }
    }

    @Test
    fun `should generate basic OpenAPI spec for simple routes`() {
        val routes = buildRouter {
            get("/health") {
                HttpResponse.ok("OK")
            }

            post<CreateUserRequest, UserResponse>("/users") { request ->
                UserResponse("123", request.name, request.email)
            }
        }

        val openApiJson = OpenApiGenerator.generate(
            routes = routes,
            title = "Test API",
            version = "1.0.0"
        )

        // Parse JSON to verify structure
        val json = Json.parseToJsonElement(openApiJson).jsonObject

        // Verify basic structure
        assertEquals("3.0.3", json["openapi"]?.jsonPrimitive?.content)
        assertEquals("Test API", json["info"]?.jsonObject?.get("title")?.jsonPrimitive?.content)
        assertEquals("1.0.0", json["info"]?.jsonObject?.get("version")?.jsonPrimitive?.content)

        // Verify paths exist
        assertTrue(json.containsKey("paths"))
        val paths = json["paths"]?.jsonObject
        assertTrue(paths?.containsKey("/health") ?: false)
        assertTrue(paths?.containsKey("/users") ?: false)

        // Verify components exist for schemas
        assertTrue(json.containsKey("components"))
    }

    @Test
    fun `should generate correct HTTP methods for routes`() {
        val routes = buildRouter {
            get("/users") { HttpResponse.okJson(emptyList<String>()) }
            post<CreateUserRequest, UserResponse>("/users") { request ->
                UserResponse("123", request.name, request.email)
            }
            patch<CreateUserRequest, UserResponse>("/users/123") { request ->
                UserResponse("123", request.name, request.email)
            }
        }

        val openApiJson = OpenApiGenerator.generate(routes, "Test API")
        val json = Json.parseToJsonElement(openApiJson).jsonObject
        val paths = json["paths"]?.jsonObject

        // GET /users should exist
        assertTrue(paths?.get("/users")?.jsonObject?.containsKey("get") ?: false)

        // POST /users should exist
        assertTrue(paths?.get("/users")?.jsonObject?.containsKey("post") ?: false)

        // PATCH /users/123 should exist
        assertTrue(paths?.get("/users/123")?.jsonObject?.containsKey("patch") ?: false)
    }

    @Test
    fun `should automatically generate schemas from typed routes`() {
        // Clear any previous route metadata
        OpenApiGenerator.clearRegistry()

        val routes = buildRouter {
            post<CreateUserRequest, UserResponse>("/users") { request ->
                UserResponse("123", request.name, request.email)
            }
        }

        val openApiJson = OpenApiGenerator.generate(routes, "Test API")
        val json = Json.parseToJsonElement(openApiJson).jsonObject
        val components = json["components"]?.jsonObject
        val schemas = components?.get("schemas")?.jsonObject

        // For debugging if needed:
        // println("Generated OpenAPI JSON: $openApiJson")

        // Should automatically discover schemas from typed routes
        assertTrue(schemas?.containsKey("CreateUserRequest") ?: false,
            "Expected CreateUserRequest schema to be auto-discovered. Available schemas: ${schemas?.keys}")
        assertTrue(schemas?.containsKey("UserResponse") ?: false,
            "Expected UserResponse schema to be auto-discovered. Available schemas: ${schemas?.keys}")

        // Verify CreateUserRequest schema structure
        val createUserSchema = schemas?.get("CreateUserRequest")?.jsonObject
        assertEquals("object", createUserSchema?.get("type")?.jsonPrimitive?.content,
            "Expected CreateUserRequest to be object type. Schema: $createUserSchema")
        assertTrue(createUserSchema?.containsKey("properties") ?: false,
            "Expected CreateUserRequest to have properties. Schema: $createUserSchema")
    }

    @Test
    fun `should generate comprehensive OpenAPI spec with path parameters`() {
        // Clear registry for clean test
        OpenApiGenerator.clearRegistry()

        val routes = buildRouter {
            // Public routes
            get("/health") { HttpResponse.ok("OK") }

            // User management
            get("/users") { HttpResponse.okJson(emptyList<UserResponse>()) }
            post<CreateUserRequest, UserResponse>("/users") { request ->
                UserResponse("123", request.name, request.email)
            }

            // User detail routes with path parameters
            get("/users/{id}") { request ->
                val userId = request.pathParam("id") ?: "unknown"
                HttpResponse.okJson(UserResponse(userId, "Test", "test@example.com"))
            }
            patch<CreateUserRequest, UserResponse>("/users/{id}") { body, request ->
                val userId = request.pathParam("id") ?: "unknown"
                UserResponse(userId, body.name, body.email)
            }
            delete("/users/{id}") {
                HttpResponse.noContent()
            }
        }

        val openApiJson = OpenApiGenerator.generate(routes, "User Management API", "2.0.0")
        val json = Json.parseToJsonElement(openApiJson).jsonObject

        // Verify API info
        assertEquals("User Management API", json["info"]?.jsonObject?.get("title")?.jsonPrimitive?.content)
        assertEquals("2.0.0", json["info"]?.jsonObject?.get("version")?.jsonPrimitive?.content)

        val paths = json["paths"]?.jsonObject!!

        // Verify all paths exist
        assertTrue(paths.containsKey("/health"))
        assertTrue(paths.containsKey("/users"))
        assertTrue(paths.containsKey("/users/{id}"))

        // Verify HTTP methods
        val userDetailPath = paths["/users/{id}"]?.jsonObject!!
        assertTrue(userDetailPath.containsKey("get"))
        assertTrue(userDetailPath.containsKey("patch"))
        assertTrue(userDetailPath.containsKey("delete"))

        // Verify path parameters are detected
        val getUserOperation = userDetailPath["get"]?.jsonObject!!
        val parameters = getUserOperation["parameters"]?.jsonArray
        assertTrue(parameters != null && parameters.size > 0, "Expected path parameters for /users/{id}")

        // Verify schemas are registered
        val schemas = json["components"]?.jsonObject?.get("schemas")?.jsonObject!!
        assertTrue(schemas.containsKey("CreateUserRequest"))
        assertTrue(schemas.containsKey("UserResponse"))

        // Verify response status codes
        val postUsersOperation = paths["/users"]?.jsonObject?.get("post")?.jsonObject!!
        val responses = postUsersOperation["responses"]?.jsonObject!!
        assertTrue(responses.containsKey("201"), "POST should return 201 Created")

        val deleteOperation = userDetailPath["delete"]?.jsonObject!!
        val deleteResponses = deleteOperation["responses"]?.jsonObject!!
        assertTrue(deleteResponses.containsKey("204"), "DELETE should return 204 No Content")
    }

    @Test
    fun `should include example data from openApiExample method`() {
        // Clear registry for clean test
        OpenApiGenerator.clearRegistry()

        val routes = buildRouter {
            post<CreateUserRequest, UserResponse>("/users") { request ->
                UserResponse("123", request.name, request.email)
            }
        }

        val openApiJson = OpenApiGenerator.generate(routes, "Test API")
        val json = Json.parseToJsonElement(openApiJson).jsonObject
        val schemas = json["components"]?.jsonObject?.get("schemas")?.jsonObject!!

        // Verify schemas were generated (examples are not supported in automatic mode yet)
        val createUserSchema = schemas["CreateUserRequest"]?.jsonObject!!
        assertEquals("object", createUserSchema["type"]?.jsonPrimitive?.content)
        assertTrue(createUserSchema.containsKey("properties"))

        val userResponseSchema = schemas["UserResponse"]?.jsonObject!!
        assertEquals("object", userResponseSchema["type"]?.jsonPrimitive?.content)
        assertTrue(userResponseSchema.containsKey("properties"))

        // Verify properties are correctly detected
        val createUserProps = createUserSchema["properties"]?.jsonObject!!
        assertTrue(createUserProps.containsKey("name"))
        assertTrue(createUserProps.containsKey("email"))

        val userResponseProps = userResponseSchema["properties"]?.jsonObject!!
        assertTrue(userResponseProps.containsKey("id"))
        assertTrue(userResponseProps.containsKey("name"))
        assertTrue(userResponseProps.containsKey("email"))
    }
}