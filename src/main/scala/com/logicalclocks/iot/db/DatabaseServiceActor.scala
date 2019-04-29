package com.logicalclocks.iot.db

import akka.actor.Actor
import akka.actor.Props
import com.logicalclocks.iot.lwm2m.IpsoObjectMeasurement
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DatabaseServiceActor() extends Actor {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  var measurementsBuffer: List[IpsoObjectMeasurement] = List.empty
  var blockedDevicesEndpointsBuffer: Set[String] = Set.empty

  def receive: Receive = ???
}

object DatabaseServiceActor {
  def props(): Props = Props(new InMemoryBufferServiceActor())

  final case class AddMeasurementsToDatabase(measurements: Iterable[IpsoObjectMeasurement])

  final object GetMeasurements

  final case class UpdateDeviceBlockStatus(endpoint: String, block: Boolean)
}

