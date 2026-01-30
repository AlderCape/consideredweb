package com.consideredweb.examples

import com.consideredweb.core.FrameworkHandler
import com.consideredweb.core.HttpResponse
import com.consideredweb.core.buildRoutes
import com.consideredweb.server.JavaHttpServer

fun main() {
    val routes = buildRoutes {
        get("/") {
            HttpResponse.ok("Hello, Web Framework!")
        }

        get("/health") {
            HttpResponse.ok("OK")
        }

        get("/users/{id}") { request ->
            val id = request.pathParam("id")
            HttpResponse.ok("""{"id": "$id", "name": "User $id"}""")
        }

        post("/users") { request ->
            println("Creating user with body: ${request.body}")
            HttpResponse.created("""{"id": "123", "name": "New User"}""")
        }

        get("/query") { request ->
            val name = request.queryParam("name") ?: "Anonymous"
            HttpResponse.ok("Hello, $name!")
        }
    }

    val server = JavaHttpServer()
    val handler = FrameworkHandler(routes)

    server.start(8080, handler)

    println("Visit: http://localhost:8080")
    println("Try: http://localhost:8080/users/42")
    println("Try: http://localhost:8080/query?name=World")
    println("Press Ctrl+C to stop")

    // Keep the server running
    Runtime.getRuntime().addShutdownHook(Thread {
        server.stop()
    })

    Thread.currentThread().join()
}