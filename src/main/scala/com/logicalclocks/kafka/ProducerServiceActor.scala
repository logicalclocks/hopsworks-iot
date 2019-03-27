package com.logicalclocks.kafka

import java.io.File

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Cancellable
import akka.actor.Props
import com.logicalclocks.db.InMemoryBufferServiceActor.GetMeasurements
import com.logicalclocks.kafka.ProducerServiceActor.PollDatabase
import com.logicalclocks.kafka.ProducerServiceActor.ReceiveMeasurements
import com.logicalclocks.lwm2m.IpsoObjectMeasurement
import org.apache.avro.Schema
import org.apache.avro.Schema.Parser
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.duration._

// TODO: this class is to be changed after implementing DatabaseService
class ProducerServiceActor(dbActor: ActorRef) extends Actor {
  import context._

  val logger: Logger = LoggerFactory.getLogger(getClass)

  var pollingCancellable: Cancellable = _

  var avroSchemas: Map[Int, Schema] = _

  val kafkaProducer: HopsKafkaProducer = HopsKafkaProducer()

  override def preStart(): Unit = {
    //TODO: call Hopsworks for the latest schemas here
    avroSchemas = loadAvroSchemasFromResource()
    logger.debug("Loaded {} avro schemas", avroSchemas.size)
    pollingCancellable = context.system.scheduler.schedule(1 second, 1 second, self, PollDatabase)
    super.preStart()
  }

  override def postStop(): Unit = {
    pollingCancellable.cancel()
    kafkaProducer.close()
    super.postStop()
  }

  def receive: Receive = {
    case ReceiveMeasurements(measurements) =>
      if (measurements.nonEmpty) {
        logger.debug("PollDatabase: {}", measurements)
        measurements.foreach(m =>
          kafkaProducer.sendIpsoObject(m, avroSchemas.get(m.objectId)))
      }
    case PollDatabase =>
      dbActor ! GetMeasurements(self)
  }

  private def loadAvroSchemasFromResource(): Map[Int, Schema] = {
    val path = getClass.getResource("/avro/lwm2m")
    val folder = new File(path.getPath)
    if (folder.exists && folder.isDirectory) {
      folder
        .listFiles
        .toList
        .map(new Parser().parse(_))
        .map(s => s.getProp("objectId").toInt -> s)
        .toMap
    } else {
      logger.warn("Loaded 0 avro schemas")
      Map.empty
    }
  }
}

object ProducerServiceActor {

  def props(dbActor: ActorRef): Props = Props(new ProducerServiceActor(dbActor))

  final object PollDatabase

  final case class ReceiveMeasurements(list: Iterable[IpsoObjectMeasurement])
}
