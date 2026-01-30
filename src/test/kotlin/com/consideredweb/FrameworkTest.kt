package com.consideredweb

import com.consideredweb.core.FrameworkHandler
import com.consideredweb.core.HttpResponse
import com.consideredweb.core.Request
import com.consideredweb.core.buildRoutes
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream
import kotlin.test.assertEquals

class FrameworkTest {

    companion object {
        @JvmStatic
        fun simepleRequests(): Stream<Arguments> = Stream.of(
            Arguments.of(
                Request(
                    method = "GET",
                    uri = "/health",
                    path = "/health",
                    queryParams = emptyMap(),
                    pathParams = emptyMap(),
                    headers = emptyMap(),
                    body = "",
                    remoteAddress = "127.0.0.1"
                ), HttpResponse.ok("OK")
            ),
            Arguments.of(
                Request(
                    method = "GET",
                    uri = "/users/123",
                    path = "/users/123",
                    queryParams = emptyMap(),
                    pathParams = emptyMap(),
                    headers = emptyMap(),
                    body = "",
                    remoteAddress = "127.0.0.1"
                ), HttpResponse.ok("User: 123")
            ),
            Arguments.of(
                Request(
                    method = "GET",
                    uri = "/nonexistent",
                    path = "/nonexistent",
                    queryParams = emptyMap(),
                    pathParams = emptyMap(),
                    headers = emptyMap(),
                    body = "",
                    remoteAddress = "127.0.0.1"
                ), HttpResponse.notFound("""{"message": "Route not found: GET /nonexistent", "code": "ROUTE_NOT_FOUND"}""")
            )
        )
    }

    @ParameterizedTest
    @MethodSource("simepleRequests")
    fun `test basic route matching`(req: Request, res: HttpResponse) {
        val routes = buildRoutes {
            get("/health") { HttpResponse.ok("OK") }
            get("/users/{id}") { request ->
                val id = request.pathParam("id")
                HttpResponse.ok("User: $id")
            }
            post("/users") { HttpResponse.created("User created") }
        }

        val handler = FrameworkHandler(routes)

        val actualResponse = handler.handle(req)
        assertEquals(res.status, actualResponse.status)
        assertEquals(res.body, actualResponse.body)
    }
}