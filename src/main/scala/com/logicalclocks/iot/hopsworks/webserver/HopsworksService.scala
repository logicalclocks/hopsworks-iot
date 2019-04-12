package com.logicalclocks.iot.hopsworks.webserver

import akka.actor.ActorRef
import akka.http.scaladsl.model.ContentTypes
import akka.http.scaladsl.model.HttpEntity
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.StandardRoute
import akka.pattern.ask
import akka.util.Timeout
import com.logicalclocks.iot.leshan.LeshanActor.GetConnectedDevices
import com.logicalclocks.iot.leshan.devices.IotDevice

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.Future
import spray.json._
import com.logicalclocks.iot.commons.HopsworksJsonProtocol._
import com.logicalclocks.iot.leshan.LeshanActor.GetLeshanConfig
import com.logicalclocks.iot.leshan.LeshanConfig

trait HopsworksService {

  implicit val leshanActor: ActorRef

  implicit val timeout: Timeout = Timeout(5 seconds)

  val route: Route = {
    pathPrefix("nodes") {
      (pathEnd & get) {
        val devicesF: Future[Set[IotDevice]] = ask(leshanActor, GetConnectedDevices).mapTo[Set[IotDevice]]
        val devices: Set[IotDevice] = Await.result(devicesF, timeout.duration)
        val json: JsValue = devices.toList.sortBy(_.endpoint).toJson
        completeJson(json)
      } ~
        (path(Segment / "ignored") & post) { deviceId =>
          complete("/nodes/" + deviceId + "/ignored")
        }
    } ~
      (path("jwt") & post) {
        complete("/jwt")
      } ~
      (pathEndOrSingleSlash & get) {
        val configF: Future[LeshanConfig] = ask(leshanActor, GetLeshanConfig).mapTo[LeshanConfig]
        val config: LeshanConfig = Await.result(configF, timeout.duration)
        completeJson(config.toJson)
      }
  }

  private def completeJson(json: JsValue): StandardRoute =
    complete(HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, json.toString)))

}
