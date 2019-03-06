package com.logicalclocks.leshan

import akka.actor.Actor
import akka.actor.Props
import com.logicalclocks.leshan.LeshanActor.AskForId
import com.logicalclocks.leshan.LeshanActor.StartServer
import org.eclipse.leshan.server.registration.Registration
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class LeshanActor(config: LeshanConfig) extends Actor {
  val logger: Logger = LoggerFactory.getLogger(getClass)
  val server: HopsLeshanServer = new HopsLeshanServer(config, self)

  def receive: Receive = {
    case StartServer => {
      server.createAndStartServer()
    }
    case AskForId(reg) => {
      val id = server.askForId(reg)
      logger.info("Asked for ID. Got {}", id)
    }
  }

}

object LeshanActor {

  def props(config: LeshanConfig): Props = Props(new LeshanActor(config))

  final object StartServer

  final case class AskForId(reg: Registration)

}
