package com.consideredweb.server

import com.consideredweb.core.HttpHandler
import com.consideredweb.core.HttpResponse
import com.consideredweb.core.HttpServer
import com.consideredweb.core.Request
import com.sun.net.httpserver.HttpExchange
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import com.sun.net.httpserver.HttpServer as JdkHttpServer

private val logger = LoggerFactory.getLogger(JavaHttpServer::class.java)

class JavaHttpServer : HttpServer {
    private var server: JdkHttpServer? = null

    override fun start(port: Int, handler: HttpHandler) {
        server = JdkHttpServer.create(InetSocketAddress(port), 0).apply {
            createContext("/") { exchange ->
                Thread.startVirtualThread {
                    try {
                        val request = exchange.toRequest()
                        val response = handler.handle(request)
                        exchange.sendResponse(response)
                    } catch (e: Exception) {
                        logger.error("Error handling request: {}", e.message, e)
                        try {
                            exchange.sendResponseHeaders(500, 0)
                            exchange.responseBody.close()
                        } catch (ignored: Exception) {
                            // Ignore errors when sending error response
                        }
                    }
                }
            }
            start()
        }
        logger.info("Server started on http://localhost:{}", port)
    }

    override fun stop() {
        server?.stop(0)
        server = null
        logger.info("Server stopped")
    }
}

private fun HttpExchange.toRequest(): Request {
    val uri = requestURI
    val path = uri.path
    val queryParams = parseQueryParams(uri.query)

    return Request(
        method = requestMethod,
        uri = uri.toString(),
        path = path,
        queryParams = queryParams,
        pathParams = emptyMap(), // Will be filled by FrameworkHandler
        headers = requestHeaders.mapValues { it.value.joinToString(", ") },
        body = requestBody.bufferedReader().use { it.readText() },
        remoteAddress = remoteAddress.address.hostAddress
    )
}

private fun HttpExchange.sendResponse(response: HttpResponse) {
    // Set response headers
    response.headers.forEach { (name, value) ->
        responseHeaders.set(name, value)
    }

    // Set content type if not already set
    if (!responseHeaders.containsKey("Content-Type")) {
        responseHeaders.set("Content-Type", response.contentType)
    }

    // Send response - use binary data if present, otherwise convert text to bytes
    val bodyBytes = response.bodyBytes ?: response.body.toByteArray(Charsets.UTF_8)
    sendResponseHeaders(response.status, bodyBytes.size.toLong())

    if (bodyBytes.isNotEmpty()) {
        responseBody.write(bodyBytes)
    }

    responseBody.close()
}

private fun parseQueryParams(query: String?): Map<String, String> {
    if (query.isNullOrBlank()) return emptyMap()

    return query.split("&")
        .mapNotNull { param ->
            val parts = param.split("=", limit = 2)
            if (parts.size == 2) {
                parts[0] to parts[1]
            } else null
        }
        .toMap()
}