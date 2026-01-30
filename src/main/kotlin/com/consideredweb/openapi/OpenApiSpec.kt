package com.consideredweb.openapi

import kotlinx.serialization.Serializable

@Serializable
data class OpenApiSpec(
    val openapi: String = "3.0.3",
    val info: Info,
    val paths: Map<String, PathItem>,
    val components: Components? = null,
    val security: List<SecurityRequirement>? = null
)

@Serializable
data class Info(
    val title: String,
    val version: String
)

@Serializable
data class PathItem(
    val get: Operation? = null,
    val post: Operation? = null,
    val put: Operation? = null,
    val patch: Operation? = null,
    val delete: Operation? = null,
    val head: Operation? = null,
    val options: Operation? = null
)

@Serializable
data class Operation(
    val summary: String? = null,
    val parameters: List<Parameter>? = null,
    val requestBody: RequestBody? = null,
    val responses: Map<String, Response>,
    val security: List<SecurityRequirement>? = null
)

@Serializable
data class Parameter(
    val name: String,
    val `in`: String, // "path", "query", "header", "cookie"
    val required: Boolean = false,
    val schema: Schema
)

@Serializable
data class RequestBody(
    val content: Map<String, MediaType>,
    val required: Boolean = false
)

@Serializable
data class Response(
    val description: String = "",
    val content: Map<String, MediaType>? = null
)

@Serializable
data class MediaType(
    val schema: Schema
)

@Serializable
data class Schema(
    val type: String? = null,
    val format: String? = null,
    val properties: Map<String, Schema>? = null,
    val required: List<String>? = null,
    val items: Schema? = null,
    val `$ref`: String? = null,
    val example: String? = null
)

@Serializable
data class Components(
    val schemas: Map<String, Schema>? = null,
    val securitySchemes: Map<String, SecurityScheme>? = null
)

@Serializable
data class SecurityScheme(
    val type: String,
    val scheme: String? = null,
    val bearerFormat: String? = null
)

// SecurityRequirement is just a map of security scheme names to scopes
typealias SecurityRequirement = Map<String, List<String>>