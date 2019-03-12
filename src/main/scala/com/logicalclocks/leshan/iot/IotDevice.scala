package com.logicalclocks.leshan.iot

import org.eclipse.leshan.server.registration.Registration

case class IotDevice(
  reg: Registration,
  )
{
  val endpoint: String = reg.getEndpoint
}
