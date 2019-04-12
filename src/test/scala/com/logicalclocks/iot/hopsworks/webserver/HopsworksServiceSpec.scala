package com.logicalclocks.iot.hopsworks.webserver

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.http.scaladsl.testkit.ScalatestRouteTest
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
  implicit val leshanActor: ActorRef = null

  override protected def beforeAll(): Unit = {
    super.beforeAll()
  }
}
