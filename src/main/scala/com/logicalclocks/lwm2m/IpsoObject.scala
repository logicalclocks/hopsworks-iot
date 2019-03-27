package com.logicalclocks.lwm2m

sealed trait IpsoObject

case class TempIpsoObject(
  sensorValue: Double,
  minMeasuredValue: Option[Double],
  maxMeasuredValue: Option[Double],
  minRangeValue: Option[Double],
  maxRangeValue: Option[Double],
  sensorUnits: Option[String],
  resetMinAndMaxMeasuredValues: Option[Boolean]) extends IpsoObject
