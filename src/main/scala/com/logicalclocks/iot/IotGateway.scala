package com.logicalclocks.iot

import akka.actor.ActorRef
import akka.actor.ActorSystem
import akka.actor.Terminated
import com.logicalclocks.iot.db.DatabaseServiceActor
import com.logicalclocks.iot.db.DomainDb.StopDb
import com.logicalclocks.iot.hopsworks.HopsworksServiceActor
import com.logicalclocks.iot.hopsworks.HopsworksServiceActor.StartHopsworksServer
import com.logicalclocks.iot.kafka.ProducerServiceActor
import com.logicalclocks.iot.kafka.ProducerServiceActor.StopProducer
import com.logicalclocks.iot.leshan.LeshanActor
import com.logicalclocks.iot.leshan.LeshanActor.StartServer
import com.logicalclocks.iot.leshan.LeshanConfig
import com.typesafe.config.ConfigFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration.Duration

object IotGateway extends App {
  val logger: Logger = LoggerFactory.getLogger(getClass)
  val config = ConfigFactory.load()

  val system: ActorSystem = ActorSystem("iotGateway")

  val dbActor: ActorRef =
    system.actorOf(DatabaseServiceActor.props("h2hopsworks"))

  val producerActor: ActorRef =
    system.actorOf(ProducerServiceActor.props(dbActor))

  val leshanActor: ActorRef =
    system.actorOf(LeshanActor.props(dbActor))

  val hopsworksActor: ActorRef =
    system.actorOf(HopsworksServiceActor.props(
      leshanActor,
      dbActor,
      producerActor))

  leshanActor ! StartServer
  hopsworksActor ! StartHopsworksServer

  Runtime.getRuntime.addShutdownHook(new Thread(() => {
    producerActor ! StopProducer
    dbActor ! StopDb
    val terminate: Future[Terminated] = system.terminate()
    Await.result(terminate, Duration("10 seconds"))
  }))

}
