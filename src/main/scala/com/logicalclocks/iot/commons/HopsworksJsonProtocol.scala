package com.logicalclocks.iot.commons

import com.logicalclocks.iot.leshan.devices.IotDevice
import spray.json.DefaultJsonProtocol
import spray.json.JsNumber
import spray.json.JsObject
import spray.json.JsString
import spray.json.JsValue
import spray.json.RootJsonFormat

object HopsworksJsonProtocol extends DefaultJsonProtocol {
  implicit object IotDeviceJsonFormat extends RootJsonFormat[IotDevice] {
    def write(obj: IotDevice): JsValue =
      JsObject(
        "endpoint" -> JsString(obj.endpoint),
        "ip-address" -> JsString(obj.reg.getAddress.getHostAddress),
        "port" -> JsNumber(obj.reg.getSocketAddress.getPort))

    def read(json: JsValue): IotDevice = ???
  }
}
