package com.logicalclocks.iot.lwm2m

import com.sksamuel.avro4s.AvroProp

sealed trait Measurement {
  def timestamp: Long
  def endpointClientName: String
  def objectId: Int
  def instanceId: Int
  def gatewayName: String
  def ipsoObject: IpsoObject
}

@AvroProp("objectId", "3303")
case class TempMeasurement(
  timestamp: Long,
  endpointClientName: String,
  instanceId: Int,
  gatewayName: String,
  ipsoObject: TempIpsoObject) extends Measurement {
  override val objectId: Int = 3303
}

@AvroProp("objectId", " 3302")
case class PresenceMeasurement(
  timestamp: Long,
  endpointClientName: String,
  instanceId: Int,
  gatewayName: String,
  ipsoObject: PresenceIpsoObject) extends Measurement {
  override val objectId: Int = 3302
}

// for unit-testing purposes
case class GenericMeasurement(
  timestamp: Long,
  endpointClientName: String,
  instanceId: Int,
  gatewayName: String,
  objectId: Int,
  ipsoObject: IpsoObject) extends Measurement

