# Consideredweb

A lightweight, type-safe HTTP framework for Kotlin. Provides a clean DSL for routing, middleware (filters), and automatic JSON serialization/deserialization.

## Quick Start

```kotlin
import com.consideredweb.core.*
import com.consideredweb.server.JettyHttpServer

fun main() {
    val routes = buildRouter {
        get("/hello") { HttpResponse.ok("Hello, World!") }
        get("/users/{id}") { request ->
            HttpResponse.ok("User: ${request.pathParam("id")}")
        }
    }
    JettyHttpServer().start(8080, FrameworkHandler(routes))
}
```

## Features

### Routing

```kotlin
val routes = buildRouter {
    get("/health") { HttpResponse.ok("OK") }
    post("/users") { request -> HttpResponse.created("User created") }
    put("/users/{id}") { request -> HttpResponse.ok("Updated") }
    delete("/users/{id}") { request -> HttpResponse.noContent() }
}
```

### Typed Routes with JSON

```kotlin
@Serializable
data class User(val id: String, val name: String)

val routes = buildRouter {
    getJson<List<User>>("/users") { userService.findAll() }
    post<CreateUserRequest, User>("/users") { body -> userService.create(body) }
}
```

### Filters (Middleware)

```kotlin
val routes = buildRouter {
    filter(Filters.cors(allowedOrigins = listOf("http://localhost:3000")))
    filter(Filters.logging())

    group("/api", authFilter) {
        get("/protected") { HttpResponse.ok("secret") }
    }
}
```

### Binary Responses

Serve files, images, PDFs, or any binary content:

```kotlin
get("/download") { _ ->
    val bytes = File("report.pdf").readBytes()
    HttpResponse.okBinary(bytes, "application/pdf")
        .withHeader("Content-Disposition", "attachment; filename=\"report.pdf\"")
}

get("/image") { _ ->
    val bytes = File("logo.png").readBytes()
    HttpResponse.okBinary(bytes, "image/png")
}
```

### Static File Serving

Serve static files and single-page applications:

```kotlin
import com.consideredweb.static.*

val routes = buildRouter {
    // Serve files from a directory
    static("/assets", "./public")

    // Serve a single file
    file("/favicon.ico", "./public/favicon.ico")

    // SPA mode: serves index.html for missing paths
    static("/", "./dist", spaMode = true)
}
```

### Path Parameters

```kotlin
get("/users/{id}") { request ->
    val id: String = request.pathParam("id")!!
    HttpResponse.ok("User: $id")
}

// Catch-all wildcard for remaining path segments
get("/files/{*path}") { request ->
    val path = request.pathParam("path") // e.g., "docs/2024/report.pdf"
    // ...
}
```

### Query Parameters & Headers

```kotlin
get("/search") { request ->
    val query: String? = request.queryParam("q")
    val auth: String? = request.header("Authorization")
    // ...
}
```

## Observability

### Logging

The framework uses SLF4J for logging. Add your preferred implementation:

```kotlin
// Logback (recommended)
implementation("ch.qos.logback:logback-classic:1.4.14")

// Or Log4j2
implementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.22.0")
```

Configure log levels in your logging config:
- `com.consideredweb` - Framework logs (INFO for production, DEBUG for development)
- `com.consideredweb.trace` - Method tracing (TRACE level)

### Correlation IDs

Request tracing is enabled by default via the correlation ID filter:

```kotlin
val routes = buildRouter {
    filter(Filters.correlationId())  // Enabled by default

    get("/api/data") { request ->
        // Access correlation ID in handlers
        val correlationId = request.correlationId
        HttpResponse.ok("data")
    }
}
```

The filter:
- Propagates incoming `X-Correlation-ID`, `X-Request-ID`, or `X-Trace-ID` headers
- Generates a UUID if no correlation ID is provided
- Adds `X-Correlation-ID` to all responses

**Customization:**

```kotlin
// Custom configuration
filter(Filters.correlationId(CorrelationIdConfig(
    requestHeaders = listOf("X-My-Trace-ID"),  // Headers to check
    responseHeader = "X-My-Trace-ID",          // Response header name
    generator = { "req-${System.nanoTime()}" } // Custom ID generator
)))

// Disable correlation IDs (not recommended)
// Simply don't add the filter
```

### Request Logging

```kotlin
val routes = buildRouter {
    filter(Filters.correlationId())
    filter(Filters.logging())         // INFO: method, path, status, duration

    // Detailed logging with configurable options
    filter(Filters.requestLogging(
        logHeaders = true,   // Log headers at DEBUG level
        logBody = false,     // Log bodies at TRACE level (disabled by default)
        maxBodyLength = 200  // Truncate body output
    ))

    // ... routes
}
```

The framework does not bundle any logging configuration files - you control logging via your own SLF4J implementation config.

## Installation

Add to your `build.gradle.kts`:

```kotlin
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
    // Core framework (uses built-in Java HTTP server)
    implementation("com.consideredweb:consideredweb:0.3.0")

    // Optional: Add Jetty for JettyHttpServer
    implementation("org.eclipse.jetty:jetty-server:11.0.18")
    implementation("org.eclipse.jetty:jetty-servlet:11.0.18")
    implementation("jakarta.servlet:jakarta.servlet-api:5.0.0")
}
```

### Minimal Setup (No Jetty)

For the smallest dependency footprint, use `JavaHttpServer` which only requires the JDK:

```kotlin
dependencies {
    implementation("com.consideredweb:consideredweb:0.3.0")
}
```

```kotlin
import com.consideredweb.server.JavaHttpServer

val routes = buildRouter { /* ... */ }
JavaHttpServer().start(8080, FrameworkHandler(routes))
```

## License & Usage

This project is licensed under the [MIT License](./LICENSE).

### Feedback & Adoption
While you're free to use this framework however you like under MIT,
I'd love to know where it ends up!

If you're using ConsideredWeb in a project, please consider:
- Opening a quick issue titled "Usage: [Your Project]"
- Dropping me an email at johan@aldercape.com
- Or simply starring the repo on GitHub

This helps guide the project roadmap and ensures development stays aligned with real-world use cases.
