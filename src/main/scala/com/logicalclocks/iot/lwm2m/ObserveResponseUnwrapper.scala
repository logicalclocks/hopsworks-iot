package com.logicalclocks.iot.lwm2m

import org.eclipse.leshan.core.node.LwM2mObject
import org.eclipse.leshan.core.node.LwM2mObjectInstance
import org.eclipse.leshan.core.response.ObserveResponse

import scala.collection.JavaConverters._
import scala.collection.Map
import scala.util.Success
import scala.util.Try

case class ObserveResponseUnwrapper(
  timestamp: Long,
  endpointClientName: String,
  response: ObserveResponse) {

  private val instances: Iterable[LwM2mObjectInstance] = response
    .getContent
    .asInstanceOf[LwM2mObject]
    .getInstances
    .asScala
    .values

  def getIpsoObjectList: Iterable[IpsoObjectMeasurement] =
    response.getContent.getId match {
      case 3303 =>
        instances.flatMap(extractTempFromInstance)
      case _ => throw new Error("Unknown ipso object")
    }

  private def extractTempFromInstance(instance: LwM2mObjectInstance): Option[IpsoObjectMeasurement] = {
    val resources: Map[Integer, AnyRef] = instance
      .getResources
      .asScala
      .mapValues(_.getValue)

    def tempIpsoObject: TempIpsoObject = TempIpsoObject(
      resources(5700).asInstanceOf[Double],
      resources.get(5601).map(_.asInstanceOf[Double]),
      resources.get(5602).map(_.asInstanceOf[Double]),
      resources.get(5603).map(_.asInstanceOf[Double]),
      resources.get(5604).map(_.asInstanceOf[Double]),
      resources.get(5701).map(_.asInstanceOf[String]),
      resources.get(5605).map(_.asInstanceOf[Boolean]))

    Try(tempIpsoObject)
      .map(TempIpsoObjectMeasurement(timestamp, endpointClientName, instance.getId, _)) match {
        case Success(x) => Some(x)
        case _ => None
      }
  }
}
