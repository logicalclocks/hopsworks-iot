package com.logicalclocks.leshan

import akka.actor.Actor
import akka.actor.Props
import com.logicalclocks.leshan.LeshanActor.StartServer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class LeshanActor(config: LeshanConfig) extends Actor {
  val logger: Logger = LoggerFactory.getLogger(getClass)
  val server: LeshanServer = new LeshanServer(config)

  def receive: Receive = {
    case StartServer => {
      logger.info("Start the fucking server: " + config)
      server.createAndStartServer()
    }
  }

}

object LeshanActor {

  def props(config: LeshanConfig): Props = Props(new LeshanActor(config))

  case object StartServer

}
