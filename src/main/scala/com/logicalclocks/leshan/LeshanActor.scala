package com.logicalclocks.leshan

import akka.actor.Actor
import akka.actor.Props
import com.logicalclocks.leshan.LeshanActor.ObserveTemp
import com.logicalclocks.leshan.LeshanActor.DisconnectDevice
import com.logicalclocks.leshan.LeshanActor.NewDevice
import com.logicalclocks.leshan.LeshanActor.NewObserveResponse
import com.logicalclocks.leshan.LeshanActor.StartServer
import com.logicalclocks.leshan.iot.IotDevice
import org.eclipse.leshan.core.response.ObserveResponse
import org.eclipse.leshan.server.registration.Registration
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class LeshanActor(config: LeshanConfig) extends Actor {
  val logger: Logger = LoggerFactory.getLogger(getClass)
  val server: HopsLeshanServer = new HopsLeshanServer(config, self)

  var connectedDevices: Set[IotDevice] = Set.empty

  def receive: Receive = {
    case StartServer =>
      server.createAndStartServer()
    case NewDevice(reg) =>
      logger.info(s"New device connected with endpoint ${reg.getEndpoint}")
      connectedDevices = connectedDevices + IotDevice(reg)
      // automatically observe the temp value
      self ! ObserveTemp(reg)
    case ObserveTemp(reg) =>
      val tempObservation = server.observeRequest(reg, 3303)
      logger.debug(
        "Adding tempObservation status {} for {}",
        tempObservation.isSuccess,
        reg.getEndpoint)
    case DisconnectDevice(endpoint) =>
      connectedDevices = connectedDevices.filterNot(_.endpoint == endpoint)
      logger.debug(s"Disconnect device with endpoint $endpoint. " +
        s"Currently connected devices ${connectedDevices.size}")
    case NewObserveResponse(endpoint, resp, timestamp) =>
      logger.debug(s"New data from $endpoint at time $timestamp: observation $resp")
  }

}

object LeshanActor {

  def props(config: LeshanConfig): Props = Props(new LeshanActor(config))

  final object StartServer

  final case class NewDevice(reg: Registration)

  final case class ObserveTemp(reg: Registration)

  final case class DisconnectDevice(endpoint: String)

  final case class NewObserveResponse(endpoint: String, response: ObserveResponse, timestamp: Long)
}
