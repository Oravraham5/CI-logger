package com.fyber.marketplace

import com.fyber.mobile.exchange.forest.mocks.endpoints.AutomationLoggerHandler
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Verticle
import io.vertx.core.Vertx
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.json.JsonObject
import io.vertx.ext.healthchecks.HealthCheckHandler
import io.vertx.ext.healthchecks.HealthChecks
import io.vertx.ext.web.Router
import mu.KotlinLogging

class VertxHandler : AbstractVerticle() {

    private val logger = KotlinLogging.logger {}
    private var server: HttpServer? = null

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            Vertx.vertx().deployVerticle(VertxHandler())
        }
    }

    override fun start(startFuture: Future<Void>?) {
        val vertx = if (getVertx() == null) Vertx.vertx() else getVertx()
        val port = 5000
        val options = HttpServerOptions()
        options.setCompressionSupported(true).isDecompressionSupported = true
        options.logActivity = true
        vertx.createHttpServer(options).requestHandler(requestHandler).exceptionHandler { t -> logger.error(t) { "Exception raised attempting to handle HTTP requests" } }.listen(port) { res ->
            if (res.succeeded()) {
                logger.info { "HTTP Server started, listening on port ${port}" }
                server = res.result()
                startFuture?.complete()
            } else {
                logger.error(res.cause()) { "Failed starting the http server" }
                startFuture?.fail(res.cause())
            }
        }
    }
    private val requestHandler: Router
        get() {
            logger.info { "starting to configure the route handlers" }

            val vertx = getVertx() ?: Vertx.vertx()
            val router = Router.router(vertx)
            router.route().failureHandler { ctx ->
                val msg = ctx.failure()?.message ?: "Unknown error"
                val errCode = if (ctx.statusCode() == 401) ctx.statusCode() else 500
                logger.error(ctx.failure()) { "a request has failed! ${msg}"  }
                val json = JsonObject().put("timestamp", System.nanoTime()).put("status", ctx.statusCode()).put("failure", HttpResponseStatus.valueOf(if (ctx.statusCode() >= 0) ctx.statusCode() else 500).reasonPhrase()).put("path", ctx.request().path()).put("message", msg)
                ctx.response().putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                ctx.response().setStatusCode(errCode).end(json.encodePrettily())
            }

            AutomationLoggerHandler.appendRoute(router, vertx)

            val checks = HealthChecks.create(vertx)
            router.get("/node-health-check").blockingHandler(HealthCheckHandler.createWithHealthChecks(checks), false)

            return router
        }
}