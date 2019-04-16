package com.logicalclocks.iot.hopsworks.webserver

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.http.scaladsl.testkit.ScalatestRouteTest
import com.logicalclocks.iot.leshan.LeshanActor
import com.logicalclocks.iot.leshan.LeshanConfig
import org.eclipse.leshan.LwM2m
import org.scalatest.Matchers
import org.scalatest.WordSpec

class HopsworksServiceSpec extends WordSpec
  with Matchers
  with ScalatestRouteTest
  with HopsworksService {

  val actorSystem: ActorSystem = ActorSystem("iotGateway__Test")
  val leshanConfig: LeshanConfig = LeshanConfig(
    "localhost",
    LwM2m.DEFAULT_COAP_SECURE_PORT,
    "localhost",
    LwM2m.DEFAULT_COAP_PORT)
  val leshanActor: ActorRef =
    actorSystem.actorOf(LeshanActor.props(leshanConfig, null))
  val hopsworksServiceActor: ActorRef = null

  override protected def beforeAll(): Unit = {
    super.beforeAll()
  }

  "HopsworksService" should {
    "return a JSON with LeshanConfig for GET requests to /gateway" in {
      Get("/gateway") ~> route ~> check {
        responseAs[String] shouldEqual
          """{"blockedDevices":[],"coapHost":"localhost","coapPort":5683,"coapsHost":"localhost","coapsPort":5684}"""
      }
    }

    "return an empty JSON array for GET requests to /gateway/nodes with no IotDevice connected" in {
      Get("/gateway/nodes") ~> route ~> check {
        responseAs[String] shouldEqual "[]"
      }
    }

  }
}
