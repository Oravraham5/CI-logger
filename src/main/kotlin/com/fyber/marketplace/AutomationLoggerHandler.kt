package com.fyber.mobile.exchange.forest.mocks.endpoints

import io.vertx.core.Handler
import io.vertx.core.MultiMap
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.sockjs.SockJSHandler
import io.vertx.ext.web.handler.sockjs.SockJSHandlerOptions
import io.vertx.ext.web.handler.sockjs.SockJSSocket
import mu.KotlinLogging
import java.security.InvalidParameterException
import java.util.*
import java.util.concurrent.TimeUnit

class AutomationLoggerHandler(val vertx: Vertx) : Handler<SockJSSocket> {

    private val logger = KotlinLogging.logger {}
    private val logs = HashMap<String, MutableList<String>>()

    companion object {
        fun appendRoute(router:Router, vertx:Vertx) {
            val handler = AutomationLoggerHandler(vertx)
            val socketHandler: SockJSHandler = SockJSHandler.create(vertx, SockJSHandlerOptions().setHeartbeatInterval(2000))
            socketHandler.socketHandler(handler)

            router.route("/api/automation-logger").blockingHandler(socketHandler)
            router.route("/api/automation-logger/*").blockingHandler(socketHandler)
            router.route("/api/automation-logger-logs/:id").blockingHandler(handler.loggerDumpHandler)


            //REST API
            router.route("/api/automation-logger-logs/id").blockingHandler(handler.loggerlogs)

            router.route("/api/automation-logger-logs/").handler(BodyHandler.create());
            router.post("/api/automation-logger-logs/").handler(handler.postlog);

        }
    }


    override fun handle(event: SockJSSocket?) {
        logger.info { "got a web socket connection from ${event?.remoteAddress()}" }
        var identifier = UUID.randomUUID().toString()
        logs.put(identifier, ArrayList())
        logs[identifier]?.add("FOREST_LOGGING_CONNECTED")
        event?.handler {
            it?.toString()?.let { it2 ->
                if (it2.contains("F_ID=")) {
                    logs.remove(identifier)
                    identifier = it2.substring(5)
                    logs[identifier]?.add("FOREST_LOGGING_RECONNECTED FOR SESSION ID = $identifier")
                } else {
                    logs[identifier]?.add(it2);
                }
            }
        }
        event?.write(identifier)
        event?.endHandler {
            vertx.setTimer(TimeUnit.MINUTES.toMillis(15)) {
                logs.remove(identifier)
            }
        }
    }


    val postlog = Handler<RoutingContext> { request ->
        val body = request?.bodyAsJson
        logger.info { "request body: ${body}"}
        val id = body?.getString("id")
        val log = body?.getString("log")
        if (logs[id]?.isEmpty() == true) {
            request.fail(InvalidParameterException("unknown identifier."))
        } else {
            logger.info { "Received new log for id= ${id}" }
            if (log != null) {
                logs[id]?.add(log)
            }
        }
    }



    val loggerDumpHandler = Handler<RoutingContext> { request ->
        logger.info { "got a GET request from  ${request?.request()?.remoteAddress()}"}
        val pathParam = request.pathParam("id")
        val logLines = logs[pathParam]
        if (logLines == null) {
            request.fail(InvalidParameterException("session not found"))
        } else {
            request.response().setStatusCode(200).putHeader("Content-Type", "text").end(logLines.joinToString(separator = "\n"))
        }
    }


    val loggerlogs = Handler<RoutingContext> { request ->
        logger.info { "got a GET request from  ${request?.request()?.remoteAddress()}" }
        var identifier = UUID.randomUUID().toString()

        logs.put(identifier, ArrayList())
        logs[identifier]?.add("FOREST_LOGGING_CONNECTED")

        request.response().setStatusCode(200).putHeader("Content-Type", "text").end(identifier)

        vertx.setTimer(TimeUnit.MINUTES.toMillis(15)) {
            logs.remove(identifier)
        }
    }
}