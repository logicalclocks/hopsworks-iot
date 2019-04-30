package com.logicalclocks.iot.db.slick

import cats.data.OptionT
import com.logicalclocks.iot.lwm2m.IpsoObjectMeasurement

import scala.concurrent.Future

trait HopsDbController {

  type DbSingleRecord = (Int, IpsoObjectMeasurement)

  def start: Future[Unit]

  def stop: Future[Unit]

  def addSingleRecord(measurement: IpsoObjectMeasurement): Future[Int]

  def addBatchOfRecords(measurements: List[IpsoObjectMeasurement]): Future[Int]

  def getSingleRecord: OptionT[Future, DbSingleRecord]

  def getBatchOfRecords(batchSize: Int): Future[List[DbSingleRecord]]

  def deleteSingleRecord(measurementId: Int): Future[Int]

  def deleteListOfRecords(measurementId: List[Int]): Future[Int]
}
