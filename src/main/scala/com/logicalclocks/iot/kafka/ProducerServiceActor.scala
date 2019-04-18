package com.logicalclocks.iot.kafka

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Cancellable
import akka.actor.Props
import com.logicalclocks.iot.db.InMemoryBufferServiceActor.GetMeasurements
import com.logicalclocks.iot.kafka.ProducerServiceActor.AddAvroSchema
import com.logicalclocks.iot.kafka.ProducerServiceActor.PollDatabase
import com.logicalclocks.iot.kafka.ProducerServiceActor.ReceiveMeasurements
import com.logicalclocks.iot.kafka.ProducerServiceActor.ScheduleDatabasePoll
import com.logicalclocks.iot.lwm2m.IpsoObjectMeasurement
import org.apache.avro.Schema
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.duration._

// TODO: this class is to be changed after implementing DatabaseService
class ProducerServiceActor(dbActor: ActorRef) extends Actor {
  import context._

  val logger: Logger = LoggerFactory.getLogger(getClass)

  var pollingCancellable: Cancellable = _

  var avroSchemas: Map[Int, Schema] = Map.empty

  val kafkaProducer: HopsKafkaProducer = HopsKafkaProducer()

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
      dbActor ! GetMeasurements
    case AddAvroSchema(objectId, schema) =>
      avroSchemas = avroSchemas + (objectId -> schema)
      logger.debug("Added schema for object {}. Currently schemas = {}", objectId, avroSchemas.size)
    case ScheduleDatabasePoll =>
      pollingCancellable = context.system.scheduler.schedule(1 second, 1 second, self, PollDatabase)
  }
}

object ProducerServiceActor {

  def props(dbActor: ActorRef): Props = Props(new ProducerServiceActor(dbActor))

  final object PollDatabase

  final object ScheduleDatabasePoll

  final case class ReceiveMeasurements(list: Iterable[IpsoObjectMeasurement])

  final case class AddAvroSchema(objectId: Int, schema: Schema)
}
