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
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration._

trait HopsworksService {

  val leshanActor: ActorRef
  val hopsworksServiceActor: ActorRef

  implicit val timeout: Timeout = Timeout(5 seconds)
  implicit val ec: ExecutionContext

  val route: Route =
    pathPrefix("gateway" / "nodes") {
      getNodesRoute ~
        postBlockNodeRoute ~
        deleteBlockNodeRoute
    } ~
      pathPrefix("gateway") {
        postJwtRoute ~
          getRootPathRoute
      }

  val getRootPathRoute: Route = (pathEndOrSingleSlash & get) {
    val statusF: Future[IotGatewayStatus] = for {
      config <- ask(leshanActor, GetLeshanConfig).mapTo[LeshanConfig]
      blockedDevices <- ask(leshanActor, GetBlockedEndpoints).mapTo[Set[String]]
      connectedDevices <- ask(leshanActor, GetConnectedDevices).mapTo[Set[IotDevice]]
    } yield IotGatewayStatus(config, blockedDevices, connectedDevices.size)
    val res = Await.result(statusF, timeout.duration)
    completeJson(res.toJson)
  }

  val getNodesRoute: Route = (pathEnd & get) {
    val devicesF: Future[Set[IotDevice]] = ask(leshanActor, GetConnectedDevices).mapTo[Set[IotDevice]]
    val devices: Set[IotDevice] = Await.result(devicesF, timeout.duration)
    val json: JsValue = devices.toVector.sortBy(_.endpoint).toJson
    completeJson(json)
  }

  val postBlockNodeRoute: Route = (path(Segment / "blocked") & post) {
    endpoint =>
      {
        val statusF: Future[Boolean] = ask(leshanActor, BlockDeviceWithEndpoint(endpoint, true)).mapTo[Boolean]
        val status: Boolean = Await.result(statusF, timeout.duration)
        val statusM = Map("status" -> status).toJson
        completeJson(statusM)
      }
  }

  val deleteBlockNodeRoute: Route = (path(Segment / "blocked") & delete) {
    endpoint =>
      {
        val statusF: Future[Boolean] = ask(leshanActor, BlockDeviceWithEndpoint(endpoint, false)).mapTo[Boolean]
        val status: Boolean = Await.result(statusF, timeout.duration)
        val statusM = Map("status" -> status).toJson
        completeJson(statusM)
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

case class IotGatewayStatus(config: LeshanConfig, blockedDevicesEndpoints: Set[String], connectedDevices: Int)
