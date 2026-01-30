package com.consideredweb.examples

import com.consideredweb.core.FrameworkHandler
import com.consideredweb.core.HttpResponse
import com.consideredweb.core.buildRoutes
import com.consideredweb.server.JavaHttpServer
import com.consideredweb.server.JettyHttpServer

fun main() {
    val routes = buildRoutes {
        get("/") {
            HttpResponse.ok("Hello from configurable server!")
        }

        get("/server") {
            HttpResponse.ok("Server type can be switched via environment variable")
        }

        get("/health") {
            HttpResponse.ok("OK")
        }

        get("/users/{id}") { request ->
            val id = request.pathParam("id")
            HttpResponse.ok("""{"id": "$id", "name": "User $id", "server": "configurable"}""")
        }
    }

    // Choose server implementation based on environment variable
    val serverType = System.getenv("SERVER_TYPE") ?: "java"
    val server = when (serverType.lowercase()) {
        "jetty" -> {
            println("Using Jetty HTTP Server")
            JettyHttpServer()
        }
        "java" -> {
            println("Using Java built-in HTTP Server")
            JavaHttpServer()
        }
        else -> {
            println("Unknown server type '$serverType', using Java built-in")
            JavaHttpServer()
        }
    }

    val handler = FrameworkHandler(routes)

    server.start(8080, handler)

    println("Visit: http://localhost:8080")
    println("Try: http://localhost:8080/users/42")
    println("Try: http://localhost:8080/server")
    println("Set SERVER_TYPE=jetty to use Jetty server")
    println("Press Ctrl+C to stop")

    // Keep the server running
    Runtime.getRuntime().addShutdownHook(Thread {
        server.stop()
    })

    Thread.currentThread().join()
}