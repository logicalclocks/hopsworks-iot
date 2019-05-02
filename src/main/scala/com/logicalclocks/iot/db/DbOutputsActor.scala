package com.logicalclocks.iot.db

import akka.actor.Actor
import akka.actor.Props
import akka.pattern.pipe
import akka.util.Timeout
import com.logicalclocks.iot.db.DomainDb.Add
import com.logicalclocks.iot.db.DomainDb.DeleteSingle
import com.logicalclocks.iot.db.DomainDb.GetBatch
import com.logicalclocks.iot.db.slick.H2DatabaseController
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Failure
import scala.util.Success
import scala.language.postfixOps


class DbOutputsActor(dbConfig: String) extends Actor {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  val db = H2DatabaseController(dbConfig)
  db.start

  implicit val executionContext: ExecutionContext = context.system.dispatcher
  implicit val timeout: Timeout = Timeout(5 seconds)

  override def receive: Receive = {
    case Add(measurements) =>
      db.addBatchOfRecords(measurements.toList) onComplete {
        case Success(s) => logger.debug(s"Added $s elements to database")
        case Failure(f) => logger.error(s"Error adding to db $f")
      }
    case GetBatch(batchSize) =>
      logger.debug(GetBatch.getClass.toString)
      db.getBatchOfRecords(batchSize) pipeTo sender
    case DeleteSingle(id) =>
      db.deleteSingleRecord(id) foreach { res =>
        logger.debug(s"Result deleting object $id: $res")
      }
  }
}

object DbOutputsActor {
  def props(dbConfig: String): Props = Props(new DbOutputsActor(dbConfig))
}
