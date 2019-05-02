package com.logicalclocks.iot.db.slick

import com.logicalclocks.iot.lwm2m.GenericIpsoObjectMeasurement
import com.logicalclocks.iot.lwm2m.TempIpsoObject
import com.logicalclocks.iot.lwm2m.TempIpsoObjectMeasurement
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import org.scalatest.Matchers
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.Duration

class H2DatabaseControllerSpec extends FunSuite with Matchers with BeforeAndAfterAll {

  val db = H2DatabaseController("h2mem1")

  val t1 = TempIpsoObject(1.1, Some(1.0), Some(2.0), None, None, Some("cel"), None)
  val t2 = TempIpsoObject(2.1, Some(1.0), Some(10.0), None, None, Some("cel"), None)

  val m1 = TempIpsoObjectMeasurement(111L, "kimchi", 1, t1)
  val m2 = TempIpsoObjectMeasurement(112L, "kimchi", 1, t2)

  override protected def beforeAll(): Unit = {
    val f = db.start
    Await.result(f, Duration.Inf)
    super.beforeAll()
  }

  override protected def afterAll(): Unit = {
    val f = db.stop
    Await.result(f, Duration.Inf)
    super.afterAll()
  }

  test("adding one element to the database should return 1") {
    val f = db.addSingleRecord(m1)
    f map {
      res => res shouldBe 1
    }
    Await.result(f, Duration.Inf)
  }

  test("adding and getting one element should return the element") {
    val f = db.clearTables flatMap { _ =>
      db.addSingleRecord(m1)
    } flatMap { _ =>
      db.getSingleRecord.value
    } map {
      res => res.get._2 shouldBe m1
    }
    Await.result(f, Duration.Inf)
  }

  test("adding and deleting a record should return an empty table") {
    val f = db.clearTables flatMap { _ =>
      db.addSingleRecord(m1)
    } flatMap { _ =>
      db.getTableSize
    } flatMap { res =>
      Future(res shouldBe 1)
    } flatMap { _ =>
      db.getSingleRecord.value
    } flatMap { res =>
      db.deleteSingleRecord(res.get._1)
    } flatMap { res =>
      Future(res shouldBe 1)
    } flatMap { _ =>
      db.getTableSize
    } map (_ shouldBe 0)
    Await.result(f, Duration.Inf)
  }

  test("adding two elements and getting it back as a batch should return the same elements") {
    val f = db.clearTables flatMap { _ =>
      db.addSingleRecord(m1)
    } flatMap { _ =>
      db.addSingleRecord(m2)
    } flatMap { _ =>
      db.getBatchOfRecords(100, Set.empty[Int])
    } map { list =>
      list.size shouldBe 2
      val list2 = list.map(_._2)
      list2.contains(m1) shouldBe true
      list2.contains(m2) shouldBe true
    }
  }

  test("adding an unknown object throws IllegalArgumentException") {
    val o = GenericIpsoObjectMeasurement(0L, "", 0, 0, t1)
    val f = db.addSingleRecord(o)
    ScalaFutures.whenReady(f.failed) { e =>
      e shouldBe a[IllegalArgumentException]
    }
  }
}
