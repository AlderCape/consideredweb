package com.consideredweb

import com.consideredweb.core.HttpServer
import com.consideredweb.server.JettyHttpServer

class JettyHttpServerTest : HttpServerContractTest() {
    override fun createServer(): HttpServer = JettyHttpServer()
}