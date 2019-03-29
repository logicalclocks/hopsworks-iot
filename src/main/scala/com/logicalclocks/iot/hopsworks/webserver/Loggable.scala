package com.logicalclocks.iot.hopsworks.webserver

import akka.event.Logging.LogLevel
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.RouteResult.Complete
import akka.http.scaladsl.server.directives.DebuggingDirectives
import akka.http.scaladsl.server.directives.LogEntry
import akka.http.scaladsl.server.directives.LoggingMagnet
import akka.stream.Materializer
import akka.stream.scaladsl.Sink

import scala.concurrent.ExecutionContext
import scala.concurrent.Future

/**
 * Trait modifies default akka-http logging style
 * Ref: https://stackoverflow.com/questions/32475471/how-does-one-log-akka-http-client-requests
 */
trait Loggable {

  def logRequestResult(level: LogLevel, route: Route)(implicit m: Materializer, ex: ExecutionContext): Route = {

    def myLoggingFunction(logger: LoggingAdapter)(req: HttpRequest)(res: Any): Unit = {
      val entry = res match {
        case Complete(resp) =>
          Future.successful(LogEntry(s"${req.method} ${req.uri}: ${resp.status}", level))
        case other =>
          Future.successful(LogEntry(s"$other", level))
      }

      entry.filter(_ => !req.uri.toString.contains("metrics/task?")).map(_.logTo(logger))
    }

    DebuggingDirectives.logRequestResult(LoggingMagnet(log => myLoggingFunction(log)))(route)
  }

  private def entityAsString(entity: HttpEntity)(implicit m: Materializer, ex: ExecutionContext): Future[String] = {
    entity.dataBytes
      .map(_.decodeString(entity.contentType.charsetOption.get.value))
      .runWith(Sink.head)
  }
}
