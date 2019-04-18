package com.logicalclocks.iot.db

import akka.actor.Actor
import akka.actor.Props
import com.logicalclocks.iot.db.InMemoryBufferServiceActor.AddMeasurementsToDatabase
import com.logicalclocks.iot.db.InMemoryBufferServiceActor.GetMeasurements
import com.logicalclocks.iot.db.InMemoryBufferServiceActor.UpdateDeviceBlockStatus
import com.logicalclocks.iot.kafka.ProducerServiceActor.ReceiveMeasurements
import com.logicalclocks.iot.lwm2m.IpsoObjectMeasurement
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class InMemoryBufferServiceActor() extends Actor {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  var measurementsBuffer: List[IpsoObjectMeasurement] = List.empty
  var blockedDevicesEndpointsBuffer: Set[String] = Set.empty

  def receive: Receive = {
    case AddMeasurementsToDatabase(measurements) =>
      logger.debug("DB add: {}", measurements)
      measurementsBuffer = measurementsBuffer ::: measurements.toList
    case GetMeasurements =>
      sender ! ReceiveMeasurements(measurementsBuffer)
      measurementsBuffer = List.empty
    case UpdateDeviceBlockStatus(endpoint, block) =>
      if (block) {
        blockedDevicesEndpointsBuffer = blockedDevicesEndpointsBuffer + endpoint
      } else {
        blockedDevicesEndpointsBuffer = blockedDevicesEndpointsBuffer - endpoint
      }
  }
}

object InMemoryBufferServiceActor {
  def props(): Props = Props(new InMemoryBufferServiceActor())

  final case class AddMeasurementsToDatabase(measurements: Iterable[IpsoObjectMeasurement])

  final object GetMeasurements

  final case class UpdateDeviceBlockStatus(endpoint: String, block: Boolean)
}
