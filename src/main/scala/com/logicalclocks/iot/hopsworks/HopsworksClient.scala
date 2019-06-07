package com.logicalclocks.iot.hopsworks

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.http.javadsl.model.ContentTypes
import akka.http.scaladsl.Http
import akka.http.scaladsl.HttpsConnectionContext
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
import org.apache.avro.Schema

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.Future

case class HopsworksClient(hopsworksHostname: String, hopsworksPort: Int, hopsworksServiceActor: ActorRef) {

  implicit val system: ActorSystem = ActorSystem()
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  implicit val ec: ExecutionContextExecutor = system.dispatcher

  private def executeSingleRequest(request: HttpRequest): Future[HttpResponse] = {
    val badSslConfig: AkkaSSLConfig = AkkaSSLConfig().mapSettings(s => s.withLoose(s.loose.withDisableHostnameVerification(true)))
    val badCtx: HttpsConnectionContext = Http().createClientHttpsContext(badSslConfig)
    val connectionFlow: Flow[HttpRequest, HttpResponse, Future[Http.OutgoingConnection]] =
      Http().outgoingConnectionHttps(hopsworksHostname, connectionContext = badCtx, port = hopsworksPort)

    Source.single(request)
      .via(connectionFlow)
      .runWith(Sink.head)
  }

  def downloadCerts(jwt: String, adminPassword: String, projectId: Int): Future[GatewayCertsDTO] = {
    val token: String = if (jwt.startsWith("Bearer")) {
      jwt
    } else {
      "Bearer " + jwt
    }

    val request: HttpRequest = HttpRequest(
      method = HttpMethods.POST,
      uri = "https://" + hopsworksHostname + ":" + hopsworksPort + "/hopsworks-api/api/project/" + projectId + "/downloadGatewayCert")
      .withHeaders(RawHeader("Authorization", token))
      .withEntity(ContentTypes.APPLICATION_X_WWW_FORM_URLENCODED, "password=" + adminPassword)

    executeSingleRequest(request) flatMap { response =>
      response.status match {
        case OK => Unmarshal(response.entity).to[GatewayCertsDTO]
        case _ => Future.failed(new Error(response.entity.toString))
      }
    }
  }

  def downloadKafkaTopicSchema(jwt: String, projectId: Int, topic: String): Future[SchemaDTO] = {
    val request: HttpRequest = HttpRequest(
      method = HttpMethods.GET,
      uri = "https://" + hopsworksHostname + ":" + hopsworksPort + "/hopsworks-api/api/project/" + projectId + "/kafka/" + topic + "/schema")
      .withHeaders(RawHeader("Authorization", jwt))

    executeSingleRequest(request) flatMap { response =>
      response.status match {
        case OK => Unmarshal(response.entity).to[SchemaDTO]
        case _ => Future.failed(new Error(response.entity.toString))
      }
    }
  }
}

case class SchemaDTO(schema: Schema, version: Int)
