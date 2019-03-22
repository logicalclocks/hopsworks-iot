package com.logicalclocks.lwm2m

import com.sksamuel.avro4s.AvroProp

sealed trait IpsoObjectMeasurement {
  val endpointClientName: String
  val objectId: Int
  val timestamp: Long
}

@AvroProp("objectId", "3303")
case class TempIpsoObjectMeasurement(
  timestamp: Long,
  endpointClientName: String,
  sensorValue: Double,
  minMeasuredValue: Option[Double],
  maxMeasuredValue: Option[Double],
  minRangeValue: Option[Double],
  maxRangeValue: Option[Double],
  sensorUnits: Option[String],
  resetMinAndMaxMeasuredValues: Option[Boolean]) extends IpsoObjectMeasurement {
  val objectId: Int = 3303
}
