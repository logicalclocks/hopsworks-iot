package com.logicalclocks.iot.hopsworks

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import com.logicalclocks.iot.hopsworks.HopsworksServiceActor.DownloadGatewayCertificates
import com.logicalclocks.iot.hopsworks.HopsworksServiceActor.StartHopsworksServer
import com.logicalclocks.iot.hopsworks.webserver.HopsworksServer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.util.Failure
import scala.util.Success

class HopsworksServiceActor(
  host: String,
  port: Int,
  hopsworksHostname: String,
  hopsworksPort: Int,
  leshanActor: ActorRef) extends Actor {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  val hopsworksServer = HopsworksServer(host, port,
    hopsworksHostname, hopsworksPort,
    leshanActor, self)

  val hopsworksClient = HopsworksClient(hopsworksHostname, hopsworksPort, self)

  var certs: Option[GatewayCertsDTO] = None

  def receive: Receive = {
    case StartHopsworksServer =>
      hopsworksServer.start
    case DownloadGatewayCertificates(jwt, adminPassword, projectId) =>
      hopsworksClient.downloadCerts(jwt, adminPassword, projectId) onComplete {
        case Success(res) => certs = Some(res)
        case Failure(_) => certs = None
      }
      logger.debug("new certs: " + certs)

  }

}

object HopsworksServiceActor {
  def props(
    host: String,
    port: Int,
    hopsworksHostname: String,
    hopsworksPort: Int,
    leshanActor: ActorRef): Props =
    Props(new HopsworksServiceActor(host, port, hopsworksHostname, hopsworksPort, leshanActor))

  final object StartHopsworksServer

  final case class DownloadGatewayCertificates(jwt: String, adminPassword: String, projectId: Integer)
}
