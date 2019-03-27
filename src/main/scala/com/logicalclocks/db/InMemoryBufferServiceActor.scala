package com.logicalclocks.db

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import com.logicalclocks.db.InMemoryBufferServiceActor.AddMeasurementsToDatabase
import com.logicalclocks.db.InMemoryBufferServiceActor.GetMeasurements
import com.logicalclocks.kafka.ProducerServiceActor.ReceiveMeasurements
import com.logicalclocks.lwm2m.IpsoObjectMeasurement
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class InMemoryBufferServiceActor() extends Actor {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  var measurementsBuffer: List[IpsoObjectMeasurement] = List.empty

  def receive: Receive = {
    case AddMeasurementsToDatabase(measurements) =>
      logger.debug("DB add: {}", measurements)
      measurementsBuffer = measurementsBuffer ::: measurements.toList
    case GetMeasurements(actor) =>
      actor ! ReceiveMeasurements(measurementsBuffer)
      measurementsBuffer = List.empty
  }
}

object InMemoryBufferServiceActor {
  def props(): Props = Props(new InMemoryBufferServiceActor())

  final case class AddMeasurementsToDatabase(measurements: Iterable[IpsoObjectMeasurement])

  final case class GetMeasurements(actor: ActorRef)
}
