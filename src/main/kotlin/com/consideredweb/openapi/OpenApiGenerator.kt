package com.consideredweb.openapi

import com.consideredweb.core.JsonSupport
import com.consideredweb.core.Route
import com.consideredweb.core.TypedRouteMetadata
import com.consideredweb.core.TypedRouteRegistry
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.typeOf

object OpenApiGenerator {

    fun generate(
        routes: List<Route>,
        title: String = "API",
        version: String = "1.0.0"
    ): String {
        // Automatically discover schemas from typed routes
        val discoveredSchemas = discoverSchemasFromTypedRoutes()

        val spec = OpenApiSpec(
            info = Info(title, version),
            paths = generatePaths(routes, discoveredSchemas),
            components = generateComponents(discoveredSchemas),
            security = listOf(mapOf("bearerAuth" to emptyList<String>()))
        )
        return JsonSupport.toPrettyJson(spec)
    }

    private fun generatePaths(routes: List<Route>, discoveredSchemas: Map<String, KClass<*>>): Map<String, PathItem> {
        val pathMap = mutableMapOf<String, MutableMap<String, Operation>>()

        routes.forEach { route ->
            val operations = pathMap.getOrPut(route.path) { mutableMapOf() }
            operations[route.method.name.lowercase()] = generateOperation(route, discoveredSchemas)
        }

        return pathMap.mapValues { (_, operations) ->
            PathItem(
                get = operations["get"],
                post = operations["post"],
                put = operations["put"],
                patch = operations["patch"],
                delete = operations["delete"],
                head = operations["head"],
                options = operations["options"]
            )
        }
    }

    private fun generateOperation(route: Route, discoveredSchemas: Map<String, KClass<*>>): Operation {
        // Extract path parameters
        val pathParams = RouteAnalyzer.extractPathParameters(route.path)
        val parameters = pathParams.map { param ->
            Parameter(
                name = param,
                `in` = "path",
                required = true,
                schema = Schema(type = "string")
            )
        }

        // Find typed route metadata for this route
        val typedRoute = TypedRouteRegistry.getAllRoutes().find { typed ->
            typed.method == route.method && typed.path == route.path
        }

        // Generate meaningful summary
        val summary = generateSummary(route, typedRoute)

        // Generate request body for POST/PUT/PATCH
        val requestBody = if (route.method.name.uppercase() in listOf("POST", "PUT", "PATCH")) {
            generateRequestBody(typedRoute, discoveredSchemas)
        } else null

        // Generate responses with proper content
        val responses = generateResponses(route, typedRoute, discoveredSchemas)

        // Determine if this endpoint requires authentication
        val security = if (isProtectedEndpoint(route)) {
            listOf(mapOf("bearerAuth" to emptyList<String>()))
        } else null

        return Operation(
            summary = summary,
            parameters = if (parameters.isNotEmpty()) parameters else null,
            requestBody = requestBody,
            responses = responses,
            security = security
        )
    }

    private fun generateComponents(discoveredSchemas: Map<String, KClass<*>>): Components {
        val schemas = mutableMapOf<String, Schema>()

        // Generate schemas from discovered types
        discoveredSchemas.forEach { (name, kClass) ->
            schemas[name] = generateSchemaFromKClass(kClass)
        }

        // Add security schemes
        val securitySchemes = mapOf(
            "bearerAuth" to SecurityScheme(
                type = "http",
                scheme = "bearer",
                bearerFormat = "JWT"
            )
        )

        return Components(
            schemas = if (schemas.isNotEmpty()) schemas else null,
            securitySchemes = securitySchemes
        )
    }

    /**
     * Discover all unique schema types from typed routes
     */
    private fun discoverSchemasFromTypedRoutes(): Map<String, KClass<*>> {
        val schemas = mutableMapOf<String, KClass<*>>()
        val typedRoutes = TypedRouteRegistry.getAllRoutes()

        typedRoutes.forEach { typedRoute ->
            // Add request type if present
            typedRoute.requestType?.let { kClass ->
                val name = kClass.simpleName ?: "Unknown"
                schemas[name] = kClass
            }

            // Add response type if present
            typedRoute.responseType?.let { kClass ->
                val name = kClass.simpleName ?: "Unknown"
                schemas[name] = kClass
            }

            // Extract element types from List<T> response types
            typedRoute.responseKType?.let { kType ->
                if (kType.isSubtypeOf(typeOf<List<*>>())) {
                    val arguments = kType.arguments
                    if (arguments.isNotEmpty()) {
                        val elementType = arguments.first().type
                        if (elementType != null) {
                            val elementKClass = elementType.classifier as? KClass<*>
                            val elementTypeName = elementKClass?.simpleName
                            if (elementTypeName != null && elementKClass != null) {
                                schemas[elementTypeName] = elementKClass
                            }
                        }
                    }
                }
            }
        }

        return schemas
    }

    /**
     * Generate schema from KClass using reflection
     */
    @OptIn(InternalSerializationApi::class)
    private fun generateSchemaFromKClass(kClass: KClass<*>): Schema {
        return try {
            // Use serializer for the class to generate schema
            val serializer = kClass.serializer()
            SchemaGenerator.generateSchemaFromSerializer(serializer)
        } catch (e: Exception) {
            // Fallback to basic object schema if serialization fails
            Schema(type = "object")
        }
    }

    /**
     * Generate meaningful summary for an operation
     */
    private fun generateSummary(route: Route, typedRoute: TypedRouteMetadata?): String {
        val method = route.method.name.uppercase()
        val pathParts = route.path.split("/").filter { it.isNotEmpty() && !it.startsWith("{") }

        return when {
            // Health check
            route.path == "/health" -> "Health Check"

            // Authentication routes
            route.path.startsWith("/auth/") -> {
                when (route.path) {
                    "/auth/login" -> "User Login"
                    "/auth/logout" -> "User Logout"
                    "/auth/setup-password" -> "Setup Password"
                    "/auth/generate-setup-token" -> "Generate Setup Token"
                    else -> "Authentication"
                }
            }

            // Account creation
            route.path == "/accounts" && method == "POST" -> "Create Account"

            // Resource-based summaries
            else -> {
                val resource = pathParts.lastOrNull { !it.contains("active") && !it.contains("deactivate") }?.replaceFirstChar { it.uppercase() } ?: "Resource"
                when (method) {
                    "GET" -> if (route.path.contains("/active")) "Get Active $resource" else "Get $resource"
                    "POST" -> "Create $resource"
                    "PUT" -> "Update $resource"
                    "PATCH" -> when {
                        route.path.contains("/deactivate") -> "Deactivate $resource"
                        route.path.contains("/activate") -> "Activate $resource"
                        else -> "Update $resource"
                    }
                    "DELETE" -> "Delete $resource"
                    else -> "$method $resource"
                }
            }
        }
    }

    /**
     * Generate request body specification
     */
    private fun generateRequestBody(typedRoute: TypedRouteMetadata?, discoveredSchemas: Map<String, KClass<*>>): RequestBody? {
        val requestType = typedRoute?.requestType
        return if (requestType != null) {
            val schemaName = requestType.simpleName ?: return null
            if (discoveredSchemas.containsKey(schemaName)) {
                RequestBody(
                    content = mapOf(
                        "application/json" to MediaType(
                            schema = Schema(`$ref` = "#/components/schemas/$schemaName")
                        )
                    ),
                    required = true
                )
            } else null
        } else null
    }

    /**
     * Generate response specifications with proper content
     */
    private fun generateResponses(route: Route, typedRoute: TypedRouteMetadata?, discoveredSchemas: Map<String, KClass<*>>): Map<String, Response> {
        val method = route.method.name.uppercase()
        val responseType = typedRoute?.responseType
        val responseKType = typedRoute?.responseKType

        val baseResponses = when (method) {
            "GET" -> mapOf("200" to "OK")
            "POST" -> mapOf("201" to "Created")
            "PUT", "PATCH" -> mapOf("200" to "OK")
            "DELETE" -> mapOf("204" to "No Content")
            else -> mapOf("200" to "OK")
        }

        return baseResponses.mapValues { (statusCode, description) ->
            val content = if (responseType != null && statusCode != "204") {
                generateResponseSchema(responseType, responseKType, discoveredSchemas)?.let { schema ->
                    mapOf(
                        "application/json" to MediaType(schema = schema)
                    )
                }
            } else null

            Response(
                description = description,
                content = content
            )
        }
    }

    /**
     * Generate schema for response types, handling generics like List<T>
     */
    private fun generateResponseSchema(responseType: KClass<*>, responseKType: KType?, discoveredSchemas: Map<String, KClass<*>>): Schema? {
        // Check if this is a List type with KType information
        if (responseKType != null && responseKType.isSubtypeOf(typeOf<List<*>>())) {
            // Extract the generic type parameter
            val arguments = responseKType.arguments
            if (arguments.isNotEmpty()) {
                val elementType = arguments.first().type
                if (elementType != null) {
                    val elementKClass = elementType.classifier as? KClass<*>
                    val elementTypeName = elementKClass?.simpleName
                    if (elementTypeName != null && discoveredSchemas.containsKey(elementTypeName)) {
                        return Schema(
                            type = "array",
                            items = Schema(`$ref` = "#/components/schemas/$elementTypeName")
                        )
                    }
                }
            }
        }

        // Fallback to simple class name
        val schemaName = responseType.simpleName
        return if (schemaName != null && discoveredSchemas.containsKey(schemaName)) {
            Schema(`$ref` = "#/components/schemas/$schemaName")
        } else null
    }

    /**
     * Determine if an endpoint requires authentication
     */
    private fun isProtectedEndpoint(route: Route): Boolean {
        return when {
            // Public endpoints
            route.path == "/health" -> false
            route.path == "/test-basic" -> false
            route.path.startsWith("/auth/") -> false
            route.path == "/accounts" -> false

            // All organization-scoped endpoints require auth
            route.path.startsWith("/organisations/") -> true

            else -> false
        }
    }

    /**
     * Clear the typed route registry (useful for testing)
     */
    fun clearRegistry() {
        TypedRouteRegistry.clear()
    }
}