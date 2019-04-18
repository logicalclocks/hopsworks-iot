package com.logicalclocks.iot.lwm2m

sealed trait IpsoObject

case class TempIpsoObject(
  sensorValue: Double,
  minMeasuredValue: Option[Double],
  maxMeasuredValue: Option[Double],
  minRangeValue: Option[Double],
  maxRangeValue: Option[Double],
  sensorUnits: Option[String],
  resetMinAndMaxMeasuredValues: Option[Boolean]) extends IpsoObject

case class PresenceIpsoObject(
  digitalInputState: Boolean,
  digitalInputCounter: Option[Int],
  digitalInputCounterReset: Option[Boolean],
  sensorType: Option[String],
  busyToClearDelay: Option[Int],
  clearToBusyDelay: Option[Int]) extends IpsoObject
