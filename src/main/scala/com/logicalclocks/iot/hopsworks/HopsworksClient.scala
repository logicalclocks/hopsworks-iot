package com.logicalclocks.iot.hopsworks

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.http.javadsl.model.ContentTypes
import akka.http.scaladsl.Http
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.HttpMethods
import akka.http.scaladsl.model.HttpRequest
import akka.http.scaladsl.model.HttpResponse
import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.model.headers.RawHeader
import akka.http.scaladsl.unmarshalling._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Sink
import akka.stream.scaladsl.Source
import com.logicalclocks.iot.commons.HopsworksJsonProtocol._
import com.typesafe.sslconfig.akka.AkkaSSLConfig

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.Future

case class HopsworksClient(hopsworksHostname: String, hopsworksPort: Int, hopsworksServiceActor: ActorRef) {

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  implicit val ec: ExecutionContextExecutor = system.dispatcher

  def downloadCerts(jwt: String, adminPassword: String, projectId: Int): Future[GatewayCertsDTO] = {
    val badSslConfig = AkkaSSLConfig().mapSettings(s => s.withLoose(s.loose.withDisableHostnameVerification(true)))
    val badCtx = Http().createClientHttpsContext(badSslConfig)
    val connectionFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] =
      Http().outgoingConnectionHttps(hopsworksHostname, connectionContext = badCtx, port = 8181)
    val request: HttpRequest = HttpRequest(
      method = HttpMethods.POST,
      uri = "https://" + hopsworksHostname + ":" + hopsworksPort + "/hopsworks-api/api/project/" + projectId + "/downloadGatewayCert")
      .withHeaders(RawHeader("Authorization", jwt))
      .withEntity(ContentTypes.APPLICATION_X_WWW_FORM_URLENCODED, "password=" + adminPassword)
    val responseFuture =
      Source.single(request)
        .via(connectionFlow)
        .runWith(Sink.head)
    responseFuture flatMap { response =>
      response.status match {
        case OK => Unmarshal(response.entity).to[GatewayCertsDTO]
        case _ => Future.failed(new Error(response.entity.toString))
      }
    }
  }
}
