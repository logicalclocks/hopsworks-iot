package com.logicalclocks.leshan

import akka.actor.Actor
import akka.actor.Props
import com.logicalclocks.leshan.LeshanServer.StartServer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class LeshanServer(leshanConfig: LeshanConfig) extends Actor {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  def receive: Receive = {
    case StartServer => {
      logger.info("Start the fucking server: " + leshanConfig)
    }
  }
}

object LeshanServer {

  def props(config: LeshanConfig): Props = Props(new LeshanServer(config))

  case object StartServer

}

case class LeshanConfig(
  coapsHost: String,
  coapsPort: Int,
  keyStorePath: Option[String],
  keyStoreType: Option[String],
  keyStorePass: Option[String],
  keyStoreAlias: Option[String],
  keyStoreAliasPass: Option[String])
