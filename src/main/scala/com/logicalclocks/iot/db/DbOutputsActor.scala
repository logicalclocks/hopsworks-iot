package com.logicalclocks.iot.db

import akka.actor.Actor
import akka.actor.Props
import akka.pattern.pipe
import akka.util.Timeout
import com.logicalclocks.iot.db.DomainDb.Add
import com.logicalclocks.iot.db.DomainDb.BlockEndpoint
import com.logicalclocks.iot.db.DomainDb.ClearTables
import com.logicalclocks.iot.db.DomainDb.DeleteSingle
import com.logicalclocks.iot.db.DomainDb.GetBatch
import com.logicalclocks.iot.db.DomainDb.GetBlockedEndpoints
import com.logicalclocks.iot.db.DomainDb.Stop
import com.logicalclocks.iot.db.DomainDb.UnblockEndpoint
import com.logicalclocks.iot.db.slick.H2DatabaseController
import com.logicalclocks.iot.lwm2m.Measurement
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

class DbOutputsActor(dbConfig: String) extends Actor {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  val db = H2DatabaseController(dbConfig)

  implicit val executionContext: ExecutionContext = context.system.dispatcher
  implicit val timeout: Timeout = Timeout(5 seconds)

  override def preStart(): Unit = {
    val f = db.start
    f foreach (_ => logger.debug("Start database"))
    Await.result(f, Duration.Inf)
  }

  override def postStop(): Unit =
    db.stop foreach (_ => logger.debug("Stop database"))

  override def receive: Receive = active(Set.empty[Int])

  def active(pendingForACK: Set[Int]): Receive = {
    case Add(measurements) =>
      db.addBatchOfRecords(measurements.toList) foreach { res =>
      }
    case GetBatch(batchSize) =>
      //      db.getBatchOfRecords(batchSize, pendingForACK) flatMap { batch =>
      //        if (pendingForACK.nonEmpty || batch.nonEmpty)
      //          logger.debug("Pending: " + pendingForACK + ", returned: " + batch.map(_._1))
      //        context become active(pendingForACK ++ batch.map(_._1))
      //        Future(batch)
      //      } pipeTo sender
      val f = db.getBatchOfRecords(batchSize, pendingForACK)
      val batch: List[(Int, Measurement)] = Await.result(f, Duration("5 seconds"))
      context become active(pendingForACK ++ batch.map(_._1))
      Future(batch) pipeTo sender
    case DeleteSingle(id) =>
      // TODO: make sure the operation is atomic - react to failures!
      logger.debug(s"Delete single record with id $id")
      db.deleteSingleRecord(id) foreach { _ =>
      }
      context become active(pendingForACK - id)
    case BlockEndpoint(endpoint) =>
      db.addBlockedEndpoint(endpoint) foreach (_ =>
        logger.debug(s"Block endpoint $endpoint"))
    case UnblockEndpoint(endpoint) =>
      db.deleteBlockedEndpoint(endpoint) foreach (_ =>
        logger.debug(s"Unblock endpoint $endpoint"))
    case GetBlockedEndpoints =>
      db.getBlockedEndpoints pipeTo sender
    case Stop =>
      context.system.scheduler.scheduleOnce(Duration.Zero)(System.exit(1))
    case ClearTables =>
      db.clearTables
      context become active(Set.empty[Int])
  }
}

object DbOutputsActor {
  def props(dbConfig: String): Props = Props(new DbOutputsActor(dbConfig))
}
