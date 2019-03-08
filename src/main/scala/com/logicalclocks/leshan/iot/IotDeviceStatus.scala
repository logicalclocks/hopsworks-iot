package com.logicalclocks.leshan.iot

object IotDeviceStatus extends Enumeration {
  type IotDeviceStatus = Value
  val MISSING_MAC, REGISTRATION_COMPLETE = Value
}
