package com.logicalclocks.leshan.iot

import com.logicalclocks.leshan.iot.IotDeviceStatus.IotDeviceStatus
import org.eclipse.leshan.server.registration.Registration

case class IotDevice(
  mac: String,
  reg: Registration,
  status: IotDeviceStatus) {
  val id: String = reg.getId
}
