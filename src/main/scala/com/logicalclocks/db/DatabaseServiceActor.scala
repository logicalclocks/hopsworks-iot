package com.logicalclocks.db

import akka.actor.Actor
import akka.actor.Props
import com.logicalclocks.db.DatabaseServiceActor.AddMeasurementsToDatabase
import com.logicalclocks.lwm2m.IpsoObjectMeasurement
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class DatabaseServiceActor() extends Actor {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  var measurementsBuffer: List[IpsoObjectMeasurement] = List.empty

  def receive: Receive = {
    case AddMeasurementsToDatabase(measurements) =>
      logger.debug("DB add: {}", measurements)
      measurementsBuffer = measurementsBuffer ::: measurements
  }
}

object DatabaseServiceActor {
  def props(): Props = Props(new DatabaseServiceActor)

  final case class AddMeasurementsToDatabase(measurements: List[IpsoObjectMeasurement])
}
