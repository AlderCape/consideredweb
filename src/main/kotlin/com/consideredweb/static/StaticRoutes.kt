package com.consideredweb.static

import com.consideredweb.core.HttpResponse
import com.consideredweb.core.RouterBuilder
import java.io.File

/**
 * Serve static files from a directory.
 *
 * @param urlPath The URL path prefix for static files (e.g., "/assets")
 * @param directory The file system directory to serve from
 * @param spaMode If true, serves index.html for missing paths (for single-page apps)
 */
fun RouterBuilder.static(urlPath: String, directory: String, spaMode: Boolean = false) {
    val baseDir = File(directory)
    require(baseDir.isDirectory) { "Static directory does not exist: $directory" }

    val handler = StaticFileHandler(baseDir, spaMode)
    val cleanUrlPath = urlPath.removeSuffix("/")

    get("$cleanUrlPath/{*path}") { request ->
        val path = request.pathParam("path") ?: ""
        handler.handle(path)
    }

    // Also serve the root of the url path
    if (cleanUrlPath.isNotEmpty()) {
        get(cleanUrlPath) { _ ->
            handler.handle("")
        }
    }
}

/**
 * Serve a single file at a specific URL path.
 *
 * @param urlPath The URL path to serve the file at
 * @param filePath The file system path to the file
 */
fun RouterBuilder.file(urlPath: String, filePath: String) {
    val file = File(filePath)

    get(urlPath) { _ ->
        if (!file.isFile) {
            HttpResponse.notFound("File not found")
        } else {
            try {
                val bytes = file.readBytes()
                val contentType = StaticFileHandler.getContentType(file.name)
                HttpResponse.okBinary(bytes, contentType)
            } catch (e: Exception) {
                HttpResponse.internalServerError("Error reading file: ${e.message}")
            }
        }
    }
}
