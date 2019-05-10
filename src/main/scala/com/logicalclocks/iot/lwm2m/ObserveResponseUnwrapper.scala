package com.logicalclocks.iot.lwm2m

import com.typesafe.config.ConfigFactory
import org.eclipse.leshan.core.node.LwM2mObjectInstance
import org.eclipse.leshan.core.node.LwM2mResource
import org.eclipse.leshan.core.response.ObserveResponse

import scala.collection.JavaConverters._
import scala.collection.Map
import scala.util.Success
import scala.util.Try

case class ObserveResponseUnwrapper(
  timestamp: Long,
  endpointClientName: String,
  response: ObserveResponse) {

  val gatewayId: Int = ConfigFactory.load().getInt("gateway.id")

  private val resources: Vector[LwM2mResource] = response
    .getContent
    .asInstanceOf[LwM2mObjectInstance]
    .getResources
    .asScala
    .values
    .toVector

  def getIpsoObjectList: Iterable[Measurement] =
    response.getObservation.getPath.getObjectId.toInt match {
      case 3303 =>
        //        instances.flatMap(extractTempFromInstance)
        extractTempFromInstance(resources)
      case _ => throw new Error("Unknown ipso object")
    }

  private def extractTempFromInstance(resources: Vector[LwM2mResource]): Option[Measurement] = {
    val resourcesMap: Map[Int, AnyRef] = resources
      .map { r => (r.getId, r.getValue) }.toMap

    def tempIpsoObject: TempIpsoObject = TempIpsoObject(
      resourcesMap(5700).asInstanceOf[Double],
      resourcesMap.get(5601).map(_.asInstanceOf[Double]),
      resourcesMap.get(5602).map(_.asInstanceOf[Double]),
      resourcesMap.get(5603).map(_.asInstanceOf[Double]),
      resourcesMap.get(5604).map(_.asInstanceOf[Double]),
      resourcesMap.get(5701).map(_.asInstanceOf[String]),
      resourcesMap.get(5605).map(_.asInstanceOf[Boolean]))

    Try(tempIpsoObject)
      .map(TempMeasurement(timestamp, endpointClientName, 1, gatewayId, _)) match {
        case Success(x) => Some(x)
        case _ => None
      }
  }
}
