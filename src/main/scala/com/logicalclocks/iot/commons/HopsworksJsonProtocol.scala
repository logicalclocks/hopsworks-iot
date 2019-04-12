package com.logicalclocks.iot.commons

import com.logicalclocks.iot.hopsworks.webserver.IotGatewayStatus
import com.logicalclocks.iot.leshan.LeshanConfig
import com.logicalclocks.iot.leshan.devices.IotDevice
import spray.json._
import spray.json.DefaultJsonProtocol
import spray.json.JsArray
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
        "port" -> JsNumber(obj.reg.getPort))

    def read(json: JsValue): IotDevice = ???
  }

  implicit val leshanConfigFormat = jsonFormat4(LeshanConfig)

  implicit object IotGatewayStatusFormat extends RootJsonFormat[IotGatewayStatus] {
    def write(obj: IotGatewayStatus): JsValue =
      JsObject(
        "blockedDevices" -> JsArray(obj.blockedDevicesEndpoints.toVector.map(_.toJson)),
        "coapHost" -> JsString(obj.config.coapHost),
        "coapPort" -> JsNumber(obj.config.coapPort),
        "coapsHost" -> JsString(obj.config.coapsHost),
        "coapsPort" -> JsNumber(obj.config.coapsPort))
    def read(json: JsValue): IotGatewayStatus = ???
  }
}
