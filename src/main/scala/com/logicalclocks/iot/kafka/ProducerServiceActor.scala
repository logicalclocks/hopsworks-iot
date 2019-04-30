package com.logicalclocks.iot.kafka

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Cancellable
import akka.actor.Props
import com.logicalclocks.iot.db.DatabaseServiceActor.GetMeasurements
import com.logicalclocks.iot.hopsworks.GatewayCertsDTO
import com.logicalclocks.iot.hopsworks.HopsFileWriter
import com.logicalclocks.iot.kafka.ProducerServiceActor.AddAvroSchema
import com.logicalclocks.iot.kafka.ProducerServiceActor.PollDatabase
import com.logicalclocks.iot.kafka.ProducerServiceActor.ReceiveMeasurements
import com.logicalclocks.iot.kafka.ProducerServiceActor.ScheduleDatabasePoll
import com.logicalclocks.iot.kafka.ProducerServiceActor.StopProducer
import com.logicalclocks.iot.kafka.ProducerServiceActor.UpdateCerts
import com.logicalclocks.iot.lwm2m.IpsoObjectMeasurement
import org.apache.avro.Schema
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

class ProducerServiceActor(dbActor: ActorRef) extends Actor {
  import context._

  val logger: Logger = LoggerFactory.getLogger(getClass)

  var pollingCancellable: Option[Cancellable] = None

  var avroSchemas: Map[Int, Schema] = Map.empty

  var kafkaProducer: Option[HopsKafkaProducer] = None

  var currentCerts: Option[Certs] = None

  val fileWriter = HopsFileWriter()

  override def preStart(): Unit = {
    fileWriter.createFolder().unsafeRunSync()
  }

  override def postStop(): Unit = {
    pollingCancellable.foreach(_ cancel ())
    kafkaProducer.foreach(_.close())
    val removed = fileWriter.cleanUp()
    logger.debug("Cleaned up files: " + removed.unsafeRunSync())
  }

  def receive: Receive = {
    case ReceiveMeasurements(measurements) =>
      if (measurements.nonEmpty) {
        measurements.foreach(m =>
          kafkaProducer.foreach(_.sendIpsoObject(m, avroSchemas.get(m.objectId))))
      }
    case PollDatabase =>
      dbActor ! GetMeasurements
    case AddAvroSchema(objectId, schema) =>
      avroSchemas = avroSchemas + (objectId -> schema)
      logger.debug("Added schema for object {}. Currently schemas = {}", objectId, avroSchemas.size)
    case ScheduleDatabasePoll =>
      pollingCancellable = Some(context.system.scheduler.schedule(1 second, 1 second, self, PollDatabase))
    case UpdateCerts(certs) =>
      val (kPath, tPath) = fileWriter.saveCertsToFiles(certs).unsafeRunSync()
      currentCerts = Some(Certs(kPath, tPath, certs.password))
      kafkaProducer = Some(HopsKafkaProducer(kPath, tPath, certs.password))
    case StopProducer =>
      implicit val executionContext: ExecutionContext = context.system.dispatcher
      context.system.scheduler.scheduleOnce(Duration.Zero)(System.exit(1))
  }
}

object ProducerServiceActor {

  def props(dbActor: ActorRef): Props = Props(new ProducerServiceActor(dbActor))

  final object PollDatabase

  final object ScheduleDatabasePoll

  final case class ReceiveMeasurements(list: Iterable[IpsoObjectMeasurement])

  final case class AddAvroSchema(objectId: Int, schema: Schema)

  final case class UpdateCerts(certsDTO: GatewayCertsDTO)

  final object StopProducer
}

sealed case class Certs(kPath: String, tPath: String, pass: String)
