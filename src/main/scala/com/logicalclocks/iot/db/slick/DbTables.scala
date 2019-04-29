package com.logicalclocks.iot.db.slick

import slick.jdbc.H2Profile.api._

object DbTables {

  val measurementsTQ = TableQuery[IpsoObjectMeasurementTable]
  val tempMeasurementsTQ = TableQuery[TempIpsoObjectTable]

  val tables = List(measurementsTQ, tempMeasurementsTQ)

  case class IpsoObjectMeasurementRow(
    id: Int,
    timestamp: Long,
    endpointClientName: String,
    instanceId: Int,
    objectId: Int)

  class IpsoObjectMeasurementTable(tag: Tag) extends Table[IpsoObjectMeasurementRow](tag, "MEASUREMENTS") {
    def id = column[Int]("ID", O.AutoInc)

    def timestamp = column[Long]("TIMESTAMP")

    def endpointClientName = column[String]("ENDPOINT_CLIENT_NAME")

    def instanceId = column[Int]("INSTANCE_ID")

    def objectId = column[Int]("OBJECT_ID")

    def * =
      (id, timestamp, endpointClientName, instanceId, objectId) <> (
        (IpsoObjectMeasurementRow.apply _).tupled,
        IpsoObjectMeasurementRow.unapply _)

    def pk = primaryKey("PK_MEASUREMENTS", id)
  }

  sealed trait IpsoObjectRow

  case class TempIpsoObjectRow(
    id: Int,
    sensorValue: Double,
    minMeasuredValue: Option[Double],
    maxMeasuredValue: Option[Double],
    minRangeValue: Option[Double],
    maxRangeValue: Option[Double],
    sensorUnits: Option[String],
    resetMinAndMaxMeasuredValues: Option[Boolean]) extends IpsoObjectRow

  class TempIpsoObjectTable(tag: Tag) extends Table[TempIpsoObjectRow](tag, "TEMP_IPSO_OBJ") {
    def measurementId = column[Int]("MEASUREMENT_ID", O.PrimaryKey)

    def sensorValue = column[Double]("SENSOR_VALUE")

    def minMeasuredValue = column[Option[Double]]("MIN_MEASURED_VALUE")

    def maxMeasuredValue = column[Option[Double]]("MAX_MEASURED_VALUE")

    def minRangeValue = column[Option[Double]]("MIN_RANGE_VALUE")

    def maxRangeValue = column[Option[Double]]("MAX_RANGE_VALUE")

    def sensorUnits = column[Option[String]]("SENSOR_UNITS")

    def resetMinAndMaxMeasuredValues = column[Option[Boolean]]("RESET_MIN_AND_MAX_MEASURED_VALUES")

    def * = (
      measurementId,
      sensorValue,
      minMeasuredValue,
      maxMeasuredValue,
      minRangeValue,
      maxRangeValue,
      sensorUnits,
      resetMinAndMaxMeasuredValues) <> (
        TempIpsoObjectRow.tupled,
        TempIpsoObjectRow.unapply _)

    def supplier = foreignKey("MEASUREMENT_FK", measurementId, measurementsTQ)(_.id, onUpdate = ForeignKeyAction
      .Restrict, onDelete = ForeignKeyAction.Cascade)
  }
}