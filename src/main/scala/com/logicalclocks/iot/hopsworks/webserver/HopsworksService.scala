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
import com.logicalclocks.iot.db.DomainDb.BlockDeviceByEndpoint
import com.logicalclocks.iot.db.DomainDb.GetBlockedDevices
import com.logicalclocks.iot.db.DomainDb.UnblockDeviceByEndpoint
import com.logicalclocks.iot.hopsworks.HopsworksServiceActor.DownloadGatewayCertificates
import com.logicalclocks.iot.leshan.LeshanActor.GetConnectedDevices
import com.logicalclocks.iot.leshan.LeshanActor.GetLeshanConfig
import com.logicalclocks.iot.leshan.LeshanConfig
import com.logicalclocks.iot.leshan.devices.IotDevice
import spray.json._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

trait HopsworksService {

  def leshanActor: ActorRef

  def hopsworksServiceActor: ActorRef

  def dbActor: ActorRef

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
      blockedDevices <- ask(dbActor, GetBlockedDevices).mapTo[Seq[String]]
      connectedDevices <- ask(leshanActor, GetConnectedDevices).mapTo[Set[IotDevice]]
    } yield IotGatewayStatus(config, blockedDevices.toSet, connectedDevices.size)
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
        ask(dbActor, BlockDeviceByEndpoint(endpoint))
        completeJson(Map("status" -> true).toJson)
      }
  }

  val deleteBlockNodeRoute: Route = (path(Segment / "blocked") & delete) {
    endpoint =>
      {
        ask(dbActor, UnblockDeviceByEndpoint(endpoint))
        completeJson(Map("status" -> false).toJson)
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
