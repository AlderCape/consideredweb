package com.consideredweb.server

import com.consideredweb.core.HttpHandler
import com.consideredweb.core.HttpResponse
import com.consideredweb.core.HttpServer
import com.consideredweb.core.Request
import jakarta.servlet.http.HttpServlet
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletHolder
import org.slf4j.LoggerFactory
import java.util.concurrent.Executors

private val logger = LoggerFactory.getLogger(JettyHttpServer::class.java)

class JettyHttpServer : HttpServer {
    private var server: Server? = null
    private val virtualThreadExecutor = Executors.newVirtualThreadPerTaskExecutor()

    override fun start(port: Int, handler: HttpHandler) {
        server = Server(port).apply {
            val context = ServletContextHandler().apply {
                contextPath = "/"
                addServlet(ServletHolder(FrameworkServlet(handler)), "/*")
            }
            this.handler = context
            start()
        }
        logger.info("Jetty server started on http://localhost:{}", port)
    }

    override fun stop() {
        server?.stop()
        server = null
        virtualThreadExecutor.shutdown()
        logger.info("Jetty server stopped")
    }

    private inner class FrameworkServlet(private val frameworkHandler: HttpHandler) : HttpServlet() {
        override fun service(req: HttpServletRequest, resp: HttpServletResponse) {
            // Process synchronously to ensure response is completed before servlet returns
            try {
                val request = req.toFrameworkRequest()
                logger.debug("Incoming request: {} {}", request.method, request.path)
                val response = frameworkHandler.handle(request)
                resp.sendFrameworkResponse(response)
            } catch (e: Exception) {
                logger.error("Error handling request: {}", e.message, e)
                try {
                    resp.status = 500
                    resp.writer.write("Internal server error: ${e.message}")
                } catch (ignored: Exception) {
                    // Ignore errors when sending error response
                }
            }
        }
    }
}

private fun HttpServletRequest.toFrameworkRequest(): Request {
    val queryParams = parseQueryParams(queryString)

    // Only read body for methods that typically have one, and when content length > 0
    val requestBody = when {
        method in listOf("POST", "PUT", "PATCH") && contentLength > 0 -> {
            inputStream.bufferedReader().use { it.readText() }
        }
        else -> ""
    }

    return Request(
        method = method ?: "GET",
        uri = requestURL.toString() + if (queryString != null) "?$queryString" else "",
        path = pathInfo ?: servletPath ?: "/",
        queryParams = queryParams,
        pathParams = emptyMap(), // Will be filled by FrameworkHandler
        headers = headerNames.asSequence().associateWith { getHeader(it) ?: "" },
        body = requestBody,
        remoteAddress = remoteAddr ?: "unknown"
    )
}

private fun HttpServletResponse.sendFrameworkResponse(response: HttpResponse) {
    // Set status
    status = response.status

    // Set response headers
    response.headers.forEach { (name, value) ->
        setHeader(name, value)
    }

    // Set content type if not already set
    if (getHeader("Content-Type") == null) {
        contentType = response.contentType
    }

    // Write body - use binary data if present, otherwise text
    if (response.bodyBytes != null) {
        outputStream.write(response.bodyBytes)
        outputStream.flush()
    } else {
        writer.write(response.body)
        writer.flush()
    }
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