package com.logicalclocks.iot.hopsworks

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import com.logicalclocks.iot.hopsworks.HopsworksServiceActor.DownloadGatewayCertificates
import com.logicalclocks.iot.hopsworks.HopsworksServiceActor.DownloadKafkaTopicSchemas
import com.logicalclocks.iot.hopsworks.HopsworksServiceActor.StartHopsworksServer
import com.logicalclocks.iot.hopsworks.webserver.HopsworksServer
import com.logicalclocks.iot.kafka.LwM2mTopics
import com.logicalclocks.iot.kafka.ProducerServiceActor.AddAvroSchema
import com.logicalclocks.iot.kafka.ProducerServiceActor.ScheduleDatabasePoll
import com.logicalclocks.iot.kafka.ProducerServiceActor.UpdateCerts
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContextExecutor
import scala.util.Failure
import scala.util.Success

class HopsworksServiceActor(
  host: String,
  port: Int,
  hopsworksHostname: String,
  hopsworksPort: Int,
  leshanActor: ActorRef,
  dbActor: ActorRef,
  producerServiceActor: ActorRef) extends Actor {

  implicit val ec: ExecutionContextExecutor = context.system.dispatcher

  val logger: Logger = LoggerFactory.getLogger(getClass)

  val hopsworksServer = HopsworksServer(host, port,
    hopsworksHostname, hopsworksPort,
    leshanActor, self, dbActor, context)

  val hopsworksClient = HopsworksClient(hopsworksHostname, hopsworksPort, self)

  val fileWriter: HopsFileWriter = HopsFileWriter()

  def receive: Receive = {
    case StartHopsworksServer =>
      hopsworksServer.start
    case DownloadGatewayCertificates(jwt, adminPassword, projectId) =>
      hopsworksClient.downloadCerts(jwt, adminPassword, projectId) onComplete {
        case Success(res) => {
          producerServiceActor ! UpdateCerts(res)
          self ! DownloadKafkaTopicSchemas(jwt, projectId)
        }
        case Failure(ex) => {
          logger.error("error downloading certs: " + ex.getMessage)
        }
      }
    case DownloadKafkaTopicSchemas(jwt, projectId) =>
      LwM2mTopics.values foreach (t => {
        hopsworksClient.downloadKafkaTopicSchema(jwt, projectId, t.name) onComplete {
          case Success(schemaDTO) =>
            producerServiceActor ! AddAvroSchema(t.objectId, schemaDTO.schema)
          case Failure(ex) =>
            ex.printStackTrace
        }
      })
      producerServiceActor ! ScheduleDatabasePoll
  }

}

object HopsworksServiceActor {
  def props(
    host: String,
    port: Int,
    hopsworksHostname: String,
    hopsworksPort: Int,
    leshanActor: ActorRef,
    dbActor: ActorRef,
    producerServiceActor: ActorRef): Props =
    Props(new HopsworksServiceActor(host, port, hopsworksHostname, hopsworksPort, leshanActor, dbActor, producerServiceActor))

  final object StartHopsworksServer

  final case class DownloadGatewayCertificates(jwt: String, adminPassword: String, projectId: Int)

  final case class DownloadKafkaTopicSchemas(jwt: String, projectId: Int)
}
