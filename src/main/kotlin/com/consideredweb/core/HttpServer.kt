package com.consideredweb.core

interface HttpServer {
    fun start(port: Int, handler: HttpHandler)
    fun stop()
}

fun interface HttpHandler {
    fun handle(request: Request): HttpResponse
}