package com.logicalclocks.iot.leshan

case class LeshanConfig(
  coapsHost: String,
  coapsPort: Int,
  coapHost: String,
  coapPort: Int,
  webAddress: String,
  webPort: Int)
