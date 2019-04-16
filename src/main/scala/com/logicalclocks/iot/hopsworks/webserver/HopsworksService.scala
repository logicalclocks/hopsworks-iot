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
import com.logicalclocks.iot.commons.HopsworksJsonProtocol._
import com.logicalclocks.iot.hopsworks.HopsworksServiceActor.DownloadGatewayCertificates
import com.logicalclocks.iot.leshan.LeshanActor.BlockDeviceWithEndpoint
import com.logicalclocks.iot.leshan.LeshanActor.GetBlockedEndpoints
import com.logicalclocks.iot.leshan.LeshanActor.GetConnectedDevices
import com.logicalclocks.iot.leshan.LeshanActor.GetLeshanConfig
import com.logicalclocks.iot.leshan.LeshanConfig
import com.logicalclocks.iot.leshan.devices.IotDevice
import spray.json._

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration._

trait HopsworksService {

  val leshanActor: ActorRef
  val hopsworksServiceActor: ActorRef

  implicit val timeout: Timeout = Timeout(5 seconds)

  val route: Route =
    pathPrefix("gateway" / "nodes") {
      getNodesRoute ~
        postIgnoreNodesRoute
    } ~
      pathPrefix("gateway") {
        postJwtRoute ~
          getRootPathRoute
      }

  val getRootPathRoute: Route = (pathEndOrSingleSlash & get) {
    val configF: Future[LeshanConfig] = ask(leshanActor, GetLeshanConfig).mapTo[LeshanConfig]
    val blockedDevicesF: Future[Set[String]] = ask(leshanActor, GetBlockedEndpoints).mapTo[Set[String]]
    val config: LeshanConfig = Await.result(configF, timeout.duration)
    val blockedDevices: Set[String] = Await.result(blockedDevicesF, timeout.duration)
    val status = IotGatewayStatus(config, blockedDevices)
    completeJson(status.toJson)
  }

  val getNodesRoute: Route = (pathEnd & get) {
    val devicesF: Future[Set[IotDevice]] = ask(leshanActor, GetConnectedDevices).mapTo[Set[IotDevice]]
    val devices: Set[IotDevice] = Await.result(devicesF, timeout.duration)
    val json: JsValue = devices.toVector.sortBy(_.endpoint).toJson
    completeJson(json)
  }

  val postIgnoreNodesRoute: Route = (path(Segment / "ignored") & post) {
    endpoint =>
      {
        parameters('block.as[Boolean]) { block =>
          val statusF: Future[Boolean] = ask(leshanActor, BlockDeviceWithEndpoint(endpoint, block)).mapTo[Boolean]
          val status: Boolean = Await.result(statusF, timeout.duration)
          val statusM = Map("status" -> status).toJson
          completeJson(statusM)
        }
      }
  }

  val postJwtRoute: Route = (path("jwt") & post) {
    formFields('jwt, 'projectId.as[Int]) { (jwt, projectId) =>
      hopsworksServiceActor ! DownloadGatewayCertificates(jwt, "admin", projectId)
      completeJson(Map("message" -> "ok").toJson)
    }
  }

  private def completeJson(json: JsValue): StandardRoute =
    complete(HttpResponse(entity = HttpEntity(ContentTypes.`application/json`, json.toString)))

}

case class IotGatewayStatus(config: LeshanConfig, blockedDevicesEndpoints: Set[String])
