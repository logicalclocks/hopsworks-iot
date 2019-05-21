package com.logicalclocks.iot.leshan

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import com.logicalclocks.iot.db.DomainDb.AddMeasurementsToDatabase
import com.logicalclocks.iot.leshan.LeshanActor.DisconnectDevice
import com.logicalclocks.iot.leshan.LeshanActor.GetConnectedDevices
import com.logicalclocks.iot.leshan.LeshanActor.GetLeshanConfig
import com.logicalclocks.iot.leshan.LeshanActor.NewDevice
import com.logicalclocks.iot.leshan.LeshanActor.NewObserveResponse
import com.logicalclocks.iot.leshan.LeshanActor.ObserveTemp
import com.logicalclocks.iot.leshan.LeshanActor.StartServer
import com.logicalclocks.iot.leshan.devices.IotDevice
import com.logicalclocks.iot.lwm2m.Measurement
import com.logicalclocks.iot.lwm2m.ObserveResponseUnwrapper
import com.typesafe.config.ConfigFactory
import org.eclipse.leshan.core.response.ObserveResponse
import org.eclipse.leshan.server.registration.Registration
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class LeshanActor(dbActor: ActorRef) extends Actor {
  val logger: Logger = LoggerFactory.getLogger(getClass)
  val config = LeshanConfig(
    ConfigFactory.load().getString("leshan.coapsHost"),
    ConfigFactory.load().getInt("leshan.coapsPort"),
    ConfigFactory.load().getString("leshan.coapHost"),
    ConfigFactory.load().getInt("leshan.coapPort"))

  val server: HopsLeshanServer = new HopsLeshanServer(config, self)

  def receive = active(Set.empty[IotDevice])

  def active(connectedDevices: Set[IotDevice]): Receive = {
    case StartServer =>
      server.createAndStartServer()
    case NewDevice(reg) =>
      val updated = connectedDevices + IotDevice(reg)
      logger.info(s"New device connected with endpoint ${reg.getEndpoint}.")
      logger.debug(s"Currently connected devices ${updated.size}")
      context become active(updated)
    case ObserveTemp(reg) =>
      val _ = server.observeRequest(reg, 3303)
    case DisconnectDevice(endpoint) =>
      val updated = connectedDevices.filterNot(_.endpoint == endpoint)
      logger.info(s"Disconnect device with endpoint $endpoint.")
      logger.debug(s"Currently connected devices ${updated.size}")
      context become active(updated)
    case NewObserveResponse(endpoint, resp, timestamp) =>
      val ipsoObjects: Iterable[Measurement] =
        ObserveResponseUnwrapper(timestamp, endpoint, resp)
          .getIpsoObjectList
      dbActor ! AddMeasurementsToDatabase(ipsoObjects)
    case GetConnectedDevices =>
      sender ! connectedDevices
    case GetLeshanConfig =>
      sender ! config
  }

}

object LeshanActor {

  def props(dbActor: ActorRef): Props = Props(new LeshanActor(dbActor))

  final object StartServer

  final case class NewDevice(reg: Registration)

  final case class ObserveTemp(reg: Registration)

  final case class DisconnectDevice(endpoint: String)

  final case class NewObserveResponse(endpoint: String, response: ObserveResponse, timestamp: Long)

  final object GetConnectedDevices

  final object GetLeshanConfig
}
