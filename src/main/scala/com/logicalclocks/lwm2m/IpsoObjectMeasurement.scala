package com.logicalclocks.lwm2m

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
  val objectId: Int = 3303
}

