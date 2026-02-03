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
    implementation("com.consideredweb:consideredweb:0.1.0")
}
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
