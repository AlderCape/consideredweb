package com.consideredweb

import com.consideredweb.core.HttpMethod
import com.consideredweb.core.HttpResponse
import com.consideredweb.core.RouterBuilder
import com.consideredweb.core.buildRouterAndPrint
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests for the route listing functionality.
 */
class RouteListingTest {

    @Test
    fun `should print route list correctly`() {
        // Capture console output
        val originalOut = System.out
        val outputStream = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStream))

        try {
            val builder = RouterBuilder()
            builder.apply {
                get("/health") { HttpResponse.ok("OK") }
                post("/api/users") { HttpResponse.created("Created") }
                get("/api/users/{id}") { HttpResponse.ok("User") }
                delete("/api/users/{id}") { HttpResponse.noContent() }

                group("/admin") {
                    get("/dashboard") { HttpResponse.ok("Dashboard") }
                    post("/settings") { HttpResponse.ok("Settings") }
                }
            }

            builder.printRoutes()

            val output = outputStream.toString()

            // Check that the output contains expected elements
            assertTrue(output.contains("📋 Route List:"))
            assertTrue(output.contains("=".repeat(50)))
            assertTrue(output.contains("GET     /health"))
            assertTrue(output.contains("POST    /api/users"))
            assertTrue(output.contains("GET     /api/users/{id}"))
            assertTrue(output.contains("DELETE  /api/users/{id}"))
            assertTrue(output.contains("GET     /admin/dashboard"))
            assertTrue(output.contains("POST    /admin/settings"))
            assertTrue(output.contains("Total: 6 routes"))

        } finally {
            System.setOut(originalOut)
        }
    }

    @Test
    fun `should handle empty route list`() {
        // Capture console output
        val originalOut = System.out
        val outputStream = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStream))

        try {
            val builder = RouterBuilder()
            builder.printRoutes()

            val output = outputStream.toString()
            assertTrue(output.contains("No routes defined"))

        } finally {
            System.setOut(originalOut)
        }
    }

    @Test
    fun `should sort routes alphabetically`() {
        // Capture console output
        val originalOut = System.out
        val outputStream = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStream))

        try {
            val builder = RouterBuilder()
            builder.apply {
                post("/zebra") { HttpResponse.ok("Zebra") }
                get("/alpha") { HttpResponse.ok("Alpha") }
                put("/beta") { HttpResponse.ok("Beta") }
                get("/zebra") { HttpResponse.ok("Zebra GET") }
            }

            builder.printRoutes()

            val output = outputStream.toString()
            val lines = output.split("\n").filter { it.trim().matches(Regex("\\s*\\w+\\s+/.*")) }

            // Should be sorted by path first, then by method
            assertTrue(lines[0].contains("/alpha"))
            assertTrue(lines[1].contains("/beta"))
            assertTrue(lines[2].contains("GET") && lines[2].contains("/zebra"))
            assertTrue(lines[3].contains("POST") && lines[3].contains("/zebra"))

        } finally {
            System.setOut(originalOut)
        }
    }

    @Test
    fun `buildRouterAndPrint should work correctly`() {
        // Capture console output
        val originalOut = System.out
        val outputStream = ByteArrayOutputStream()
        System.setOut(PrintStream(outputStream))

        try {
            val routes = buildRouterAndPrint {
                get("/test") { HttpResponse.ok("Test") }
                post("/api/test") { HttpResponse.created("Created") }
            }

            val output = outputStream.toString()

            // Check that routes were built correctly
            assertEquals(2, routes.size)
            assertTrue(routes.any { it.method == HttpMethod.GET && it.path == "/test" })
            assertTrue(routes.any { it.method == HttpMethod.POST && it.path == "/api/test" })

            // Check that output was printed
            assertTrue(output.contains("📋 Route List:"))
            assertTrue(output.contains("GET     /test"))
            assertTrue(output.contains("POST    /api/test"))
            assertTrue(output.contains("Total: 2 routes"))

        } finally {
            System.setOut(originalOut)
        }
    }
}