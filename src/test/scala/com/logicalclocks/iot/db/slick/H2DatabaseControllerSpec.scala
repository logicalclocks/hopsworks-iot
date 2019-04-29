package com.logicalclocks.iot.db.slick

import com.logicalclocks.iot.lwm2m.TempIpsoObject
import com.logicalclocks.iot.lwm2m.TempIpsoObjectMeasurement
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import org.scalatest.Matchers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import scala.concurrent.duration.Duration
import slick.jdbc.H2Profile.api._

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
    import DbTables._
    val q = measurementsTQ.delete
    val f = db.db.run(q) flatMap { _ =>
      db.addSingleRecord(m1)
    } flatMap { _ =>
      db.getSingleRecord.value
    } map {
      res => res.get shouldBe m1
    }
    Await.result(f, Duration.Inf)
  }
}
