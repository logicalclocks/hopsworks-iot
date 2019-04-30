package com.logicalclocks.iot.lwm2m

import com.sksamuel.avro4s.AvroProp

sealed trait IpsoObjectMeasurement {
  val timestamp: Long
  val endpointClientName: String
  val objectId: Int
  val instanceId: Int
  val ipsoObject: IpsoObject
}

@AvroProp("objectId", "3303")
case class TempIpsoObjectMeasurement(
  timestamp: Long,
  endpointClientName: String,
  instanceId: Int,
  ipsoObject: TempIpsoObject) extends IpsoObjectMeasurement {
  override val objectId: Int = 3303
}

@AvroProp("objectId", " 3302")
case class PresenceIpsoObjectMeasurement(
  timestamp: Long,
  endpointClientName: String,
  instanceId: Int,
  ipsoObject: PresenceIpsoObject) extends IpsoObjectMeasurement {
  override val objectId: Int = 3302
}

// for unit-testing purposes
case class GenericIpsoObjectMeasurement(
  timestamp: Long,
  endpointClientName: String,
  instanceId: Int,
  objectId: Int,
  ipsoObject: IpsoObject) extends IpsoObjectMeasurement

