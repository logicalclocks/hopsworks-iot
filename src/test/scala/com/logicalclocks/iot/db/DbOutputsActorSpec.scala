package com.logicalclocks.iot.db

import akka.actor.ActorSystem
import akka.testkit.ImplicitSender
import akka.testkit.TestKit
import com.logicalclocks.iot.db.DomainDb.Add
import com.logicalclocks.iot.db.DomainDb.ClearTables
import com.logicalclocks.iot.db.DomainDb.GetBatch
import com.logicalclocks.iot.lwm2m.TempIpsoObject
import com.logicalclocks.iot.lwm2m.TempMeasurement
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Matchers
import org.scalatest.WordSpecLike

class DbOutputsActorSpec
  extends TestKit(ActorSystem("DbSpec"))
  with ImplicitSender
  with WordSpecLike
  with Matchers
  with BeforeAndAfterAll {

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  val t1 = TempIpsoObject(1.1, Some(1.0), Some(2.0), None, None, Some("cel"), None)
  val t2 = TempIpsoObject(2.1, Some(1.0), Some(10.0), None, None, Some("cel"), None)

  val m1 = TempMeasurement(111L, "kimchi", 1, "gateway_1", t1)
  val m2 = TempMeasurement(112L, "kimchi", 1, "gateway_1", t2)

  "A DbOutputsActor" must {
    "clear tables correctly" in {
      val actor = system.actorOf(DbOutputsActor.props("h2mem2"))
      actor ! Add(Iterable(m1))
      expectNoMessage
      actor ! ClearTables
      expectNoMessage
      actor ! GetBatch(100)
      expectMsg(List.empty)
    }

    "store records correctly" in {
      val actor = system.actorOf(DbOutputsActor.props("h2mem2"))
      actor ! Add(Iterable(m2))
      expectNoMessage
      actor ! GetBatch(100)
      expectMsg(List((2, m2)))
      actor ! ClearTables
    }

    "return a record only once" in {
      val actor = system.actorOf(DbOutputsActor.props("h2mem2"))
      actor ! Add(Iterable(m1))
      expectNoMessage
      actor ! GetBatch(100)
      expectMsg(List((3, m1)))
      actor ! GetBatch(100)
      expectMsg(List.empty)
      actor ! Add(Iterable(m2))
      expectNoMessage
      actor ! GetBatch(100)
      expectMsg(List((4, m2)))
      actor ! ClearTables
    }

    "return multiple records" in {
      val actor = system.actorOf(DbOutputsActor.props("h2mem2"))
      actor ! Add(Iterable(m1))
      expectNoMessage
      actor ! Add(Iterable(m2))
      expectNoMessage
      actor ! GetBatch(100)
      expectMsg(List((5, m1), (6, m2)))
      actor ! GetBatch(100)
      expectMsg(List.empty)
    }
  }
}
