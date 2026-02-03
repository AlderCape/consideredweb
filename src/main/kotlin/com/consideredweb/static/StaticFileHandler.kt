package com.consideredweb.static

import com.consideredweb.core.HttpResponse
import com.consideredweb.core.Request
import java.io.File
import java.nio.file.Path

/**
 * Handles serving static files from a directory.
 *
 * Features:
 * - Security: Prevents directory traversal attacks
 * - SPA mode: Falls back to index.html for missing files
 * - Content-type detection by file extension
 */
class StaticFileHandler(
    private val baseDirectory: File,
    private val spaMode: Boolean = false
) {
    private val normalizedBase: Path = baseDirectory.canonicalFile.toPath()

    /**
     * Handle a request for a static file.
     *
     * @param relativePath The path relative to the base directory
     * @return HttpResponse with file contents or error
     */
    fun handle(relativePath: String): HttpResponse {
        val cleanPath = relativePath.removePrefix("/").ifEmpty { "index.html" }
        val requestedFile = File(baseDirectory, cleanPath)
        val normalizedPath = requestedFile.canonicalFile.toPath()

        // Security check: ensure the file is within the base directory
        if (!normalizedPath.startsWith(normalizedBase)) {
            return HttpResponse.forbidden("Access denied")
        }

        return when {
            requestedFile.isFile -> serveFile(requestedFile)
            requestedFile.isDirectory -> {
                val indexFile = File(requestedFile, "index.html")
                if (indexFile.isFile) serveFile(indexFile)
                else HttpResponse.notFound("Not found")
            }
            spaMode -> {
                // In SPA mode, serve index.html for missing paths
                val indexFile = File(baseDirectory, "index.html")
                if (indexFile.isFile) serveFile(indexFile)
                else HttpResponse.notFound("Not found")
            }
            else -> HttpResponse.notFound("Not found")
        }
    }

    private fun serveFile(file: File): HttpResponse {
        return try {
            val bytes = file.readBytes()
            val contentType = getContentType(file.name)
            HttpResponse.okBinary(bytes, contentType)
        } catch (e: Exception) {
            HttpResponse.internalServerError("Error reading file: ${e.message}")
        }
    }

    companion object {
        private val contentTypes = mapOf(
            "html" to "text/html; charset=utf-8",
            "htm" to "text/html; charset=utf-8",
            "css" to "text/css; charset=utf-8",
            "js" to "application/javascript; charset=utf-8",
            "mjs" to "application/javascript; charset=utf-8",
            "json" to "application/json; charset=utf-8",
            "xml" to "application/xml; charset=utf-8",
            "txt" to "text/plain; charset=utf-8",
            "md" to "text/markdown; charset=utf-8",

            // Images
            "png" to "image/png",
            "jpg" to "image/jpeg",
            "jpeg" to "image/jpeg",
            "gif" to "image/gif",
            "svg" to "image/svg+xml",
            "ico" to "image/x-icon",
            "webp" to "image/webp",

            // Fonts
            "woff" to "font/woff",
            "woff2" to "font/woff2",
            "ttf" to "font/ttf",
            "otf" to "font/otf",
            "eot" to "application/vnd.ms-fontobject",

            // Documents
            "pdf" to "application/pdf",

            // Archives
            "zip" to "application/zip",
            "gz" to "application/gzip",

            // Media
            "mp3" to "audio/mpeg",
            "mp4" to "video/mp4",
            "webm" to "video/webm",

            // Data
            "csv" to "text/csv; charset=utf-8",
            "wasm" to "application/wasm"
        )

        fun getContentType(filename: String): String {
            val extension = filename.substringAfterLast('.', "").lowercase()
            return contentTypes[extension] ?: "application/octet-stream"
        }
    }
}
