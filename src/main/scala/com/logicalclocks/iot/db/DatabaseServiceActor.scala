package com.logicalclocks.iot.db

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.pattern.ask
import akka.pattern.pipe
import com.logicalclocks.iot.db.DatabaseServiceSm.DatabaseServiceState
import com.logicalclocks.iot.db.DomainDb.Action
import com.logicalclocks.iot.db.DomainDb.StopDb
import com.logicalclocks.iot.db.slick.H2DatabaseController
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import DomainDb._
import akka.util.Timeout
import scala.concurrent.duration._
import scala.language.postfixOps


import scala.concurrent.Await
import scala.concurrent.ExecutionContext
import scala.concurrent.duration.Duration

class DatabaseServiceActor(dbConfig: String) extends Actor {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  val db = H2DatabaseController(dbConfig)
  db.start

  implicit val executionContext: ExecutionContext = context.system.dispatcher
  implicit val timeout: Timeout = Timeout(5 seconds)


  val dbOutputsActor: ActorRef =
    context.system.actorOf(DbOutputsActor.props("h2hopsworks"))

  override def postStop(): Unit = {
    logger.debug("Stop database")
    val f = db.stop
    Await.result(f, Duration.Inf)
    super.postStop()
  }

  def receive = active(DatabaseServiceState())

  def active(state: DatabaseServiceState): Receive = {
    case StopDb =>
      context.system.scheduler.scheduleOnce(Duration.Zero)(System.exit(1))
    case a: Action =>
      logger.debug(a.getClass.toString)
      val (newState, results) = DatabaseServiceSm
        .compose(a, sender)
        .run(state).value

      results.dbOutputs
        .foreach {
          case e: GetBatch => (dbOutputsActor ? e) pipeTo sender
          case effect => dbOutputsActor ! effect
        }

      context become active(newState)
  }
}

object DatabaseServiceActor {
  def props(dbConfig: String): Props = Props(new DatabaseServiceActor(dbConfig))
}

