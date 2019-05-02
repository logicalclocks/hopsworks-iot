package com.logicalclocks.iot.db

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import akka.pattern.ask
import akka.pattern.pipe
import akka.util.Timeout
import com.logicalclocks.iot.db.DatabaseServiceSm.DatabaseServiceState
import com.logicalclocks.iot.db.DomainDb.Action
import com.logicalclocks.iot.db.DomainDb.GetBatch
import com.logicalclocks.iot.db.DomainDb.GetBlockedEndpoints
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.language.postfixOps

class DatabaseServiceActor(dbConfig: String) extends Actor {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  implicit val executionContext: ExecutionContext = context.system.dispatcher
  implicit val timeout: Timeout = Timeout(5 seconds)

  val dbOutputsActor: ActorRef =
    context.system.actorOf(DbOutputsActor.props(dbConfig))

  def receive = active(DatabaseServiceState())

  def active(state: DatabaseServiceState): Receive = {
    case a: Action =>
      val (newState, results) = DatabaseServiceSm
        .compose(a, sender)
        .run(state).value

      results.dbOutputs
        .foreach {
          case e: GetBatch => (dbOutputsActor ? e) pipeTo sender
          case GetBlockedEndpoints => (dbOutputsActor ? GetBlockedEndpoints) pipeTo sender
          case effect => dbOutputsActor ! effect
        }

      context become active(newState)
  }
}

object DatabaseServiceActor {
  def props(dbConfig: String): Props = Props(new DatabaseServiceActor(dbConfig))
}

