package com.logicalclocks.iot.hopsworks.webserver

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.logicalclocks.iot.db.DatabaseServiceActor
import com.logicalclocks.iot.leshan.LeshanActor
import org.scalatest.Matchers
import org.scalatest.WordSpec

import scala.concurrent.ExecutionContext

class HopsworksServiceSpec extends WordSpec
  with Matchers
  with ScalatestRouteTest
  with HopsworksService {

  val actorSystem: ActorSystem = ActorSystem("iotGateway__Test")
  val ec: ExecutionContext = actorSystem.dispatcher
  val leshanActor: ActorRef =
    actorSystem.actorOf(LeshanActor.props(null))
  val hopsworksServiceActor: ActorRef = null
  val dbActor: ActorRef =
    actorSystem.actorOf(DatabaseServiceActor.props("h2mem1"))

  override protected def beforeAll(): Unit = {
    super.beforeAll()
  }

  "HopsworksService" should {
    "return a JSON with LeshanConfig for GET requests to /gateway" in {
      Get("/gateway") ~> route ~> check {
        responseAs[String] shouldEqual
          """{"blockedDevicesEndpoints":[],"coapHost":"localhost","coapPort":5683,"coapsHost":"localhost","coapsPort":5684,"connectedDevices":0}"""
      }
    }

    "return an empty JSON array for GET requests to /gateway/nodes with no IotDevice connected" in {
      Get("/gateway/nodes") ~> route ~> check {
        responseAs[String] shouldEqual "[]"
      }
    }

  }
}
