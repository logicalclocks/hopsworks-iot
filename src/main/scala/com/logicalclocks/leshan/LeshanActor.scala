package com.logicalclocks.leshan

import akka.actor.Actor
import akka.actor.Props
import com.logicalclocks.leshan.LeshanActor.AskForMAC
import com.logicalclocks.leshan.LeshanActor.DisconnectDevice
import com.logicalclocks.leshan.LeshanActor.NewObserveResponse
import com.logicalclocks.leshan.LeshanActor.StartServer
import org.eclipse.leshan.core.response.ObserveResponse
import org.eclipse.leshan.server.registration.Registration
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.compat.java8.OptionConverters._

class LeshanActor(config: LeshanConfig) extends Actor {
  val logger: Logger = LoggerFactory.getLogger(getClass)
  val server: HopsLeshanServer = new HopsLeshanServer(config, self)

  var connectedDevices: Set[IotNode] = Set.empty

  def receive: Receive = {
    case StartServer =>
      server.createAndStartServer()
    case AskForMAC(reg) =>
      server.askForMAC(reg).asScala match {
        case Some(x) =>
          logger.info("New device with mac {}", x)
          connectedDevices = connectedDevices + IotNode(reg.getId, x)
          val tempObservation = server.observeRequest(reg, 3303)
          logger.debug("Adding tempObservation status {} for {}",
            tempObservation.isSuccess,
            reg.getId)
        case None =>
          logger.error("Error trying to get MAC from {}", reg.getId)
      }
    case DisconnectDevice(id) =>
      logger.debug("Disconnect device with id {}", id)
      connectedDevices = connectedDevices.filterNot(_.id == id)
    case NewObserveResponse(id, resp) =>
      logger.debug(s"New data from $id observation $resp")
  }

}

object LeshanActor {

  def props(config: LeshanConfig): Props = Props(new LeshanActor(config))

  final object StartServer

  final case class AskForMAC(reg: Registration)

  final case class DisconnectDevice(id: String)

  final case class NewObserveResponse(id: String, response: ObserveResponse)
}
