package com.logicalclocks.iot.db

import akka.actor.Actor
import akka.actor.Props
import com.logicalclocks.iot.db.DatabaseServiceActor.AddMeasurementsToDatabase
import com.logicalclocks.iot.db.DatabaseServiceActor.GetMeasurements
import com.logicalclocks.iot.db.DatabaseServiceActor.StopDb
import com.logicalclocks.iot.db.DatabaseServiceActor.UpdateDeviceBlockStatus
import com.logicalclocks.iot.db.slick.H2DatabaseController
import com.logicalclocks.iot.kafka.ProducerServiceActor.ReceiveMeasurements
import com.logicalclocks.iot.lwm2m.IpsoObjectMeasurement
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration
import scala.util.Failure
import scala.util.Success

class DatabaseServiceActor(dbConfig: String) extends Actor {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  val db = H2DatabaseController(dbConfig)
  db.start

  implicit val executionContext: ExecutionContext = context.system.dispatcher

  var measurementsBuffer: List[IpsoObjectMeasurement] = List.empty
  var blockedDevicesEndpointsBuffer: Set[String] = Set.empty

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
      val f = db.getBatchOfRecords(100)
      val list = Await.result(f, Duration.Inf)
      sender ! ReceiveMeasurements(list.map(_._2))
      list.foreach { case (id, _) => db.deleteSingleRecord(id) }
    case UpdateDeviceBlockStatus(endpoint, block) =>
      if (block) {
        blockedDevicesEndpointsBuffer = blockedDevicesEndpointsBuffer + endpoint
      } else {
        blockedDevicesEndpointsBuffer = blockedDevicesEndpointsBuffer - endpoint
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

}

