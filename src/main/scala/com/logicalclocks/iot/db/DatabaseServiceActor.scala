package com.logicalclocks.iot.db

import akka.actor.Actor
import akka.actor.Props
import akka.pattern.pipe
import com.logicalclocks.iot.db.DatabaseServiceActor.AddMeasurementsToDatabase
import com.logicalclocks.iot.db.DatabaseServiceActor.DeleteSingleRecord
import com.logicalclocks.iot.db.DatabaseServiceActor.GetMeasurements
import com.logicalclocks.iot.db.DatabaseServiceActor.StopDb
import com.logicalclocks.iot.db.DatabaseServiceActor.UpdateDeviceBlockStatus
import com.logicalclocks.iot.db.slick.H2DatabaseController
import com.logicalclocks.iot.lwm2m.IpsoObjectMeasurement
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration.Duration
import scala.util.Failure
import scala.util.Success

class DatabaseServiceActor(dbConfig: String) extends Actor {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  val db = H2DatabaseController(dbConfig)
  db.start

  implicit val executionContext: ExecutionContext = context.system.dispatcher

  var blockedDevicesEndpointsBuffer: Set[String] = Set.empty
  var measurementsNotACKed: Set[Int] = Set.empty

  override def postStop(): Unit = {
    logger.debug("Stop database")
    val f = db.stop
    Await.result(f, Duration.Inf)
    super.postStop()
  }

  def receive: Receive = {
    case AddMeasurementsToDatabase(measurements) =>
      db.addBatchOfRecords(measurements.toList) onComplete {
        case Success(s) => logger.debug(s"Added $s elements to database")
        case Failure(f) => logger.error(s"Error adding to db $f")
      }
    case GetMeasurements =>
      db.getBatchOfRecords(measurementsNotACKed.size + 100) flatMap { list =>
        val filtered = list.filter { tuple => !measurementsNotACKed.contains(tuple._1) }
        measurementsNotACKed = measurementsNotACKed ++ filtered.map(_._1)
        Future(filtered)
      } pipeTo sender
    case UpdateDeviceBlockStatus(endpoint, block) =>
      if (block)
        blockedDevicesEndpointsBuffer = blockedDevicesEndpointsBuffer + endpoint
      else
        blockedDevicesEndpointsBuffer = blockedDevicesEndpointsBuffer - endpoint
    case DeleteSingleRecord(id) =>
      db.deleteSingleRecord(id) foreach { res =>
        logger.debug(s"Result deleting object $id: $res")
        if (res == 1)
          measurementsNotACKed = measurementsNotACKed - id
      }
    case StopDb =>
      context.system.scheduler.scheduleOnce(Duration.Zero)(System.exit(1))
  }
}

object DatabaseServiceActor {
  def props(dbConfig: String): Props = Props(new DatabaseServiceActor(dbConfig))

  final case class AddMeasurementsToDatabase(measurements: Iterable[IpsoObjectMeasurement])

  final object GetMeasurements

  final case class UpdateDeviceBlockStatus(endpoint: String, block: Boolean)

  final object StopDb

  final case class DeleteSingleRecord(id: Int)

}

