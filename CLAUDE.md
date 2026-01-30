# Consideredweb - Lightweight Kotlin HTTP Framework

## Project Overview
Consideredweb is a lightweight, type-safe HTTP framework for Kotlin. It provides a clean DSL for routing, middleware (filters), and automatic JSON serialization/deserialization.

**Repository**: [Aldercape/consideredweb](https://github.com/Aldercape/consideredweb)
**Package**: `com.consideredweb:consideredweb` on GitHub Packages

## Tech Stack
- **Language**: Kotlin 2.2.0 with Java 23
- **Serialization**: kotlinx-serialization-json
- **Server**: Jetty 11 (also includes a pure Java HTTP server)
- **Testing**: JUnit 5, ArchUnit
- **Build**: Gradle with Kotlin DSL

## Package Structure
```
com.consideredweb/
├── auth/           # JWT authentication utilities
├── core/           # Core framework (Request, Response, Routes, Filters)
├── examples/       # Example applications
├── extensions/     # Extension functions
├── openapi/        # OpenAPI spec generation
├── server/         # HTTP server implementations (Jetty, Java)
└── utils/          # Utility functions (tracing, logging)
```

## Core Concepts

### Request & Response
```kotlin
// Request contains method, path, headers, body, path/query params
data class Request(
    val method: String,
    val uri: String,
    val path: String,
    val queryParams: Map<String, String>,
    val pathParams: Map<String, String>,
    val headers: Map<String, String>,
    val body: String,
    val remoteAddress: String
)

// Response with factory methods for common status codes
HttpResponse.ok(body)              // 200
HttpResponse.created(body)         // 201
HttpResponse.noContent()           // 204
HttpResponse.badRequest(body)      // 400
HttpResponse.unauthorized(body)    // 401
HttpResponse.forbidden(body)       // 403
HttpResponse.notFound(body)        // 404
HttpResponse.internalServerError(body)  // 500
```

### Routing DSL
```kotlin
// Basic routing
val routes = buildRouter {
    get("/health") { HttpResponse.ok("OK") }
    get("/users/{id}") { request ->
        val id = request.pathParam("id")
        HttpResponse.ok("User: $id")
    }
    post("/users") { request ->
        HttpResponse.created("User created")
    }
}

// Typed routes with automatic serialization
val routes = buildRouter {
    getJson<List<User>>("/users") { userService.findAll() }
    post<CreateUserRequest, User>("/users") { body -> userService.create(body) }
    patch<UpdateUserRequest, User>("/users/{id}") { body, request ->
        val id = request.pathParam("id")
        userService.update(id, body)
    }
}
```

### Filters (Middleware)
```kotlin
// Built-in filters
val routes = buildRouter {
    filter(Filters.cors(allowedOrigins = listOf("http://localhost:3000")))
    filter(Filters.logging())
    filter(Filters.errorHandling())

    get("/api/data") { HttpResponse.ok("data") }
}

// Custom filter
val authFilter = Filter { handler ->
    HttpHandler { request ->
        val token = request.header("Authorization")
        if (token == null) {
            HttpResponse.unauthorized("Missing token")
        } else {
            handler.handle(request)
        }
    }
}

// Route groups with filters
val routes = buildRouter {
    group("/api", authFilter) {
        get("/protected") { HttpResponse.ok("secret") }
    }
}
```

### Running a Server
```kotlin
val routes = buildRouter { /* ... */ }
val handler = FrameworkHandler(routes)

// Using Jetty
val server = JettyHttpServer()
server.start(8080, handler)

// Or using pure Java HTTP server
val server = JavaHttpServer()
server.start(8080, handler)
```

## Key Files

| File | Purpose |
|------|---------|
| `core/Request.kt` | HTTP request model with path/query param access |
| `core/HttpResponse.kt` | HTTP response model with status code factories |
| `core/Route.kt` | Route definition (method, path, handler) |
| `core/RouterBuilder.kt` | DSL for building routes with filter support |
| `core/Filter.kt` | Middleware interface and common filters |
| `core/TypedRoutes.kt` | Type-safe routing with automatic JSON handling |
| `core/FrameworkHandler.kt` | Main request handler with route matching |
| `server/JettyHttpServer.kt` | Jetty-based HTTP server |
| `server/JavaHttpServer.kt` | Pure Java HTTP server |
| `auth/JwtAuth.kt` | JWT token creation and validation |
| `openapi/OpenApiGenerator.kt` | OpenAPI spec generation from routes |

## Development Commands
```bash
# Run tests
./gradlew test

# Build library
./gradlew build

# Publish to GitHub Packages (requires GITHUB_TOKEN)
./gradlew publish

# Check for compilation errors
./gradlew compileKotlin
```

## Publishing

The library is published to GitHub Packages on release. To use it:

```kotlin
// settings.gradle.kts or build.gradle.kts
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/Aldercape/consideredweb")
        credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("com.consideredweb:consideredweb:0.1.0")
}
```

## Testing Patterns
- **Unit tests**: Test routing, filters, and handlers in isolation
- **Contract tests**: `HttpServerContractTest` validates server implementations
- **Architecture tests**: ArchUnit ensures package dependencies are correct

## Common Patterns

### Path Parameters
```kotlin
get("/users/{id}") { request ->
    val id: String = request.pathParam("id")!!
    val idAsUuid: UUID = request.pathParam<UUID>("id")
}
```

### Query Parameters
```kotlin
get("/search") { request ->
    val query: String? = request.queryParam("q")
    val page: Int = request.queryParam("page")?.toInt() ?: 1
}
```

### Headers
```kotlin
get("/api/data") { request ->
    val auth: String? = request.header("Authorization")
    val contentType: String? = request.header("Content-Type")
}
```

### Response Headers
```kotlin
get("/download") { request ->
    HttpResponse.ok(content)
        .withHeader("Content-Disposition", "attachment")
        .withHeader("Cache-Control", "no-cache")
}
```

## Error Handling
The framework automatically handles exceptions in typed routes:
- `UnauthorizedException` -> 401
- `ForbiddenException` -> 403
- `NotFoundException` -> 404
- Other exceptions -> 400 Bad Request

For custom error handling, use the `Filters.errorHandling()` filter or implement your own.
