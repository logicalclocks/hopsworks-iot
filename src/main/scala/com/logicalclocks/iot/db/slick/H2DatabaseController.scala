package com.logicalclocks.iot.db.slick

import cats.data.OptionT
import cats.implicits._
import com.logicalclocks.iot.db.slick.DbTables._
import com.logicalclocks.iot.lwm2m.IpsoObjectMeasurement
import com.logicalclocks.iot.lwm2m.TempIpsoObject
import com.logicalclocks.iot.lwm2m.TempIpsoObjectMeasurement
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import slick.jdbc.H2Profile.api._
import slick.jdbc.meta.MTable

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration

case class H2DatabaseController(path: String) extends HopsDbController {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  private val db = Database.forConfig(path)

  override def getSingleRecord: OptionT[Future, DbSingleRecord] = for {
    m1 <- getTopMeasurement
    (m, t) <- joinIpsoObject(m1)
  } yield (m.id, createIpsoObjectMeasurement(m, t))

  override def getBatchOfRecords(batchSize: Int): Future[List[DbSingleRecord]] = for {
    m <- getSingleRecord.value
    seq <- getBatchOfType(m, batchSize)
  } yield seq.map { case (m, t) => (m.id, createIpsoObjectMeasurement(m, t)) }.toList

  private def getBatchOfType(m: Option[DbSingleRecord], batchSize: Int): Future[Seq[(IpsoObjectMeasurementRow, TempIpsoObjectRow)]] =
    m match {
      case Some(mm) => {
        mm._2.objectId match {
          case 3303 => {
            val q = for {
              m <- measurementsTQ
              t <- tempMeasurementsTQ if m.id === t.measurementId
            } yield (m, t)
            val action = q.take(batchSize).result
            db.run(action)
          }
        }
      }
      case _ => Future.successful(Seq.empty)
    }

  override def deleteSingleRecord(measurementId: Int): Future[Int] =
    db.run(
      measurementsTQ.filter(_.id === measurementId).delete)

  override def deleteListOfRecords(measurementId: List[Int]): Future[Int] = ???

  override def addSingleRecord(measurement: IpsoObjectMeasurement): Future[Int] = measurement.objectId match {
    case 3303 =>
      addMeasurement(measurement).flatMap(mId =>
        addTempIpsoObject(mId, measurement.ipsoObject.asInstanceOf[TempIpsoObject]))
    case _ =>
      Future.failed(new IllegalArgumentException(s"Unknown objectId ${measurement.objectId}"))
  }

  //TODO: change implementation to a single SQL query
  override def addBatchOfRecords(measurements: List[IpsoObjectMeasurement]): Future[Int] =
    seqFutures(measurements)(addSingleRecord).flatMap { l => Future(l.sum) }

  private def seqFutures[T, U](xs: TraversableOnce[T])(g: T => Future[U]): Future[List[U]] =
    xs.foldLeft(Future.successful[List[U]](Nil)) {
      (f, item) =>
        f.flatMap {
          x => g(item).map(_ :: x)
        }
    } map (_.reverse)

  def clearTables: Future[Int] =
    db.run(measurementsTQ.delete)

  def getTableSize: Future[Int] =
    db.run(measurementsTQ.length.result)

  override def start: Future[Unit] = for {
    tables <- openDbSession()
    _ <- if (tables.isEmpty) createDb()
    else Future.successful(Unit)
  } yield ()

  override def stop: Future[Unit] =
    Future(db.close)

  private def createDb(): Future[Unit] = {
    val setup = DBIO.seq((measurementsTQ.schema ++ tempMeasurementsTQ.schema).create)
    db.run(setup)
  }

  private def openDbSession(): Future[Vector[MTable]] = {
    db.createSession()
    db.run(MTable.getTables)
  }

  // $COVERAGE-OFF$
  @Deprecated
  def printContent() = {
    val action: Future[Unit] = {
      logger.info("Measurements:")
      val q1 = for (m <- measurementsTQ)
        yield LiteralColumn("  ") ++
        m.id.asColumnOf[String] ++ "\t" ++
        m.timestamp.asColumnOf[String] ++ "\t" ++
        m.endpointClientName ++ "\t" ++
        m.instanceId.asColumnOf[String] ++ "\t" ++
        m.objectId.asColumnOf[String]
      db.stream(q1.result).foreach(logger.info)
    }.flatMap { _ =>
      println("\nTemps:")
      val q2 = for (t <- tempMeasurementsTQ)
        yield LiteralColumn("  ") ++ t.measurementId.asColumnOf[String] ++ "\t" ++
        t.sensorValue.asColumnOf[String] ++ "\t" ++
        t.minMeasuredValue.getOrElse(0.0).asColumnOf[String] ++ "\t" ++
        t.maxMeasuredValue.getOrElse(0.0).asColumnOf[String] ++ "\t" ++
        t.minRangeValue.getOrElse(0.0).asColumnOf[String] ++ "\t" ++
        t.maxRangeValue.getOrElse(0.0).asColumnOf[String] ++ "\t" ++
        t.sensorUnits.getOrElse("").asColumnOf[String] ++ "\t" ++
        t.resetMinAndMaxMeasuredValues.getOrElse(false).asColumnOf[String]
      db.stream(q2.result).foreach(logger.info)
    }
    Await.result(action, Duration.Inf)
  }
  // $COVERAGE-ON$

  private def addMeasurement(obj: IpsoObjectMeasurement): Future[Int] = {
    val insertMeasurementQuery = measurementsTQ returning measurementsTQ.map(_.id) into ((row, id) => row.copy(id = id))
    val action = insertMeasurementQuery += IpsoObjectMeasurementRow(0, obj.timestamp, obj.endpointClientName,
      obj.instanceId, obj.objectId)
    db.run(action) map (res => res.id)
  }

  private def tempObjToRow(obj: TempIpsoObject)(mId: Int): TempIpsoObjectRow =
    TempIpsoObjectRow(mId, obj.sensorValue, obj.minMeasuredValue, obj.maxMeasuredValue, obj.minRangeValue,
      obj.maxRangeValue, obj.sensorUnits, obj.resetMinAndMaxMeasuredValues)

  private def addTempIpsoObject(mId: Int, obj: TempIpsoObject): Future[Int] = {
    val action = tempMeasurementsTQ += tempObjToRow(obj)(mId)
    db.run(action)
  }

  private def getTopMeasurement: OptionT[Future, IpsoObjectMeasurementRow] = {
    val q = measurementsTQ.take(1).result.headOption
    OptionT(db.run(q))
  }

  private def joinIpsoObject(m: IpsoObjectMeasurementRow): OptionT[Future, (IpsoObjectMeasurementRow, IpsoObjectRow)] = m.objectId match {
    case 3303 => {
      val a = for {
        (m, t) <- measurementsTQ join tempMeasurementsTQ on (_.id === _.measurementId)
      } yield (m, t)
      val res = a.result.headOption
      OptionT(db.run(res))
    }
  }

  private def createIpsoObjectMeasurement(m: IpsoObjectMeasurementRow, i: IpsoObjectRow): IpsoObjectMeasurement = {
    m.objectId match {
      case 3303 => {
        val t = i.asInstanceOf[TempIpsoObjectRow]
        TempIpsoObjectMeasurement(m.timestamp, m.endpointClientName, m.instanceId, TempIpsoObject(
          t.sensorValue, t.minMeasuredValue, t.maxMeasuredValue, t.minRangeValue, t.maxRangeValue, t.sensorUnits, t.resetMinAndMaxMeasuredValues))
      }
    }
  }
}
