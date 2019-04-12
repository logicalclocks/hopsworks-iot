package com.logicalclocks.iot.hopsworks.webserver

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContextExecutor

case class HopsworksServer(host: String, port: Int, leshanActor: ActorRef)
  extends HopsworksService with Loggable {

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val logger: Logger = LoggerFactory.getLogger(getClass)

  private val myLoggedRoute = logRequestResult(Logging.InfoLevel, route)

  def start =
    Http().bindAndHandle(route, host, port)

}
