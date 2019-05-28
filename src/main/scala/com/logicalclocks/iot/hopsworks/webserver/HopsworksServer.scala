package com.logicalclocks.iot.hopsworks.webserver

import akka.actor.ActorContext
import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext

case class HopsworksServer(
  host: String,
  port: Int,
  leshanActor: ActorRef,
  hopsworksServiceActor: ActorRef,
  dbActor: ActorRef,
  ac: ActorContext)
  extends HopsworksService with Loggable with CorsSupport {

  implicit val system: ActorSystem = ac.system
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val ec: ExecutionContext = ac.system.dispatcher

  val logger: Logger = LoggerFactory.getLogger(getClass)

  private val loggedRoute = logRequestResult(Logging.InfoLevel, route)

  def start =
    Http().bindAndHandle(corsHandler(loggedRoute), host, port)
}
