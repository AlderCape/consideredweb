package com.consideredweb

import com.consideredweb.core.FrameworkHandler
import com.consideredweb.core.HttpResponse
import com.consideredweb.core.HttpServer
import com.consideredweb.core.buildRoutes
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers.ofString
import java.net.http.HttpRequest.newBuilder
import java.net.http.HttpResponse.BodyHandlers
import java.util.stream.Stream
import kotlin.test.assertEquals

/**
 * Contract test for HttpServer implementations.
 * Subclasses should override createServer() to provide their specific implementation.
 */
abstract class HttpServerContractTest {

    data class RequestResponseSpec(val req: HttpRequest.Builder, val url: String, val res: HttpResponse) {
        val request: HttpRequest = req.uri(URI.create(url)).build()
    }

    companion object {
        @JvmStatic
        fun httpRequestTestCases(): Stream<RequestResponseSpec> = Stream.of(
            RequestResponseSpec(
                newBuilder().GET(),
                "http://localhost:8081/test",
                HttpResponse.ok("Test response")
            ),
            RequestResponseSpec(
                newBuilder().GET(),
                "http://localhost:8081/users/42",
                HttpResponse.ok("User: 42")
            ),
            RequestResponseSpec(
                newBuilder().POST(ofString("""{"name": "John"}""")),
                "http://localhost:8081/users",
                HttpResponse.created("User created")
            ),
            RequestResponseSpec(
                newBuilder().GET(),
                "http://localhost:8081/query?name=World",
                HttpResponse.ok("Hello, World!")
            ),
            RequestResponseSpec(
                newBuilder().GET(),
                "http://localhost:8081/nonexistent",
                HttpResponse.notFound("""{"message": "Route not found: GET /nonexistent", "code": "ROUTE_NOT_FOUND"}""")
            )
        )
    }

    /**
     * Subclasses must implement this to provide their specific HttpServer implementation
     */
    abstract fun createServer(): HttpServer

    @ParameterizedTest
    @MethodSource("httpRequestTestCases")
    fun `test server handles HTTP requests`(spec: RequestResponseSpec) = runBlocking {
        val routes = buildRoutes {
            get("/test") { HttpResponse.ok("Test response") }
            get("/users/{id}") { request ->
                val id = request.pathParam("id")
                HttpResponse.ok("User: $id")
            }
            post("/users") { HttpResponse.created("User created") }
            get("/query") { request ->
                val name = request.queryParam("name") ?: "Anonymous"
                HttpResponse.ok("Hello, $name!")
            }
        }

        val server = createServer()
        val handler = FrameworkHandler(routes)

        try {
            // Start server
            server.start(8081, handler)
            delay(100) // Give server time to start

            val client = HttpClient.newHttpClient()

            val response = client.send(spec.request, BodyHandlers.ofString())
            assertEquals(spec.res.status, response.statusCode())
            assertEquals(spec.res.body, response.body())

        } finally {
            server.stop()
        }
    }
}