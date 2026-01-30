package com.consideredweb

import com.consideredweb.core.HttpServer
import com.consideredweb.server.JavaHttpServer

class JavaHttpServerTest : HttpServerContractTest() {
    override fun createServer(): HttpServer = JavaHttpServer()
}