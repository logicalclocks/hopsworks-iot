package com.logicalclocks.iot.leshan

import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props
import com.logicalclocks.iot.db.InMemoryBufferServiceActor.AddMeasurementsToDatabase
import com.logicalclocks.iot.db.InMemoryBufferServiceActor.UpdateDeviceBlockStatus
import com.logicalclocks.iot.leshan.LeshanActor.BlockDeviceWithEndpoint
import com.logicalclocks.iot.leshan.LeshanActor.DisconnectDevice
import com.logicalclocks.iot.leshan.LeshanActor.GetBlockedEndpoints
import com.logicalclocks.iot.leshan.LeshanActor.GetConnectedDevices
import com.logicalclocks.iot.leshan.LeshanActor.GetLeshanConfig
import com.logicalclocks.iot.leshan.LeshanActor.NewDevice
import com.logicalclocks.iot.leshan.LeshanActor.NewObserveResponse
import com.logicalclocks.iot.leshan.LeshanActor.ObserveTemp
import com.logicalclocks.iot.leshan.LeshanActor.StartServer
import com.logicalclocks.iot.leshan.devices.IotDevice
import com.logicalclocks.iot.lwm2m.IpsoObjectMeasurement
import com.logicalclocks.iot.lwm2m.ObserveResponseUnwrapper
import org.eclipse.leshan.core.response.ObserveResponse
import org.eclipse.leshan.server.registration.Registration
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class LeshanActor(config: LeshanConfig, dbActor: ActorRef) extends Actor {
  val logger: Logger = LoggerFactory.getLogger(getClass)
  val server: HopsLeshanServer = new HopsLeshanServer(config, self)

  var connectedDevices: Set[IotDevice] = Set.empty

  var blockedDevicesEndpoints: Set[String] = Set.empty

  def receive: Receive = {
    case StartServer =>
      server.createAndStartServer()
    case NewDevice(reg) =>
      connectedDevices = connectedDevices + IotDevice(reg)
      logger.info(s"New device connected with endpoint ${reg.getEndpoint}." +
        s"Currently connected devices ${connectedDevices.size}")
    // automatically observe the temp value
    // stop doing in unconditionally!!
    //self ! ObserveTemp(reg)
    case ObserveTemp(reg) =>
      val _ = server.observeRequest(reg, 3303)
    case DisconnectDevice(endpoint) =>
      connectedDevices = connectedDevices.filterNot(_.endpoint == endpoint)
      logger.debug(s"Disconnect device with endpoint $endpoint. " +
        s"Currently connected devices ${connectedDevices.size}")
    case NewObserveResponse(endpoint, resp, timestamp) =>
      val ipsoObjects: Iterable[IpsoObjectMeasurement] =
        ObserveResponseUnwrapper(timestamp, endpoint, resp)
          .getIpsoObjectList
      if (!blockedDevicesEndpoints.contains(endpoint))
        dbActor ! AddMeasurementsToDatabase(ipsoObjects)
    case GetConnectedDevices =>
      sender ! connectedDevices
    case GetBlockedEndpoints =>
      sender ! blockedDevicesEndpoints
    case GetLeshanConfig =>
      sender ! config
    case BlockDeviceWithEndpoint(endpoint, block) =>
      if (block) {
        blockedDevicesEndpoints = blockedDevicesEndpoints + endpoint
      } else {
        blockedDevicesEndpoints = blockedDevicesEndpoints - endpoint
      }
      dbActor ! UpdateDeviceBlockStatus(endpoint, block)
      sender ! blockedDevicesEndpoints.contains(endpoint)
  }

}

object LeshanActor {

  def props(config: LeshanConfig, dbActor: ActorRef): Props = Props(new LeshanActor(config, dbActor))

  final object StartServer

  final case class NewDevice(reg: Registration)

  final case class ObserveTemp(reg: Registration)

  final case class DisconnectDevice(endpoint: String)

  final case class NewObserveResponse(endpoint: String, response: ObserveResponse, timestamp: Long)

  final object GetConnectedDevices

  final object GetBlockedEndpoints

  final object GetLeshanConfig

  final case class BlockDeviceWithEndpoint(endpoint: String, block: Boolean)
}
