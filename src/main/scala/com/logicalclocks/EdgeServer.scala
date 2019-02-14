package com.logicalclocks

import com.logicalclocks.kafka.HopsKafkaProducer
import org.eclipse.leshan.LwM2m
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import scopt.OParser

object EdgeServer extends App {

  val logger: Logger = LoggerFactory.getLogger(getClass)
  val builder = OParser.builder[Config]
  val parser1 = {
    import builder._
    OParser.sequence(
      programName("Hops IoT Edge Server"),
      head("Hops IoT Edge Server"),
      opt[String]('l', "coapshost")
        .action((x,c) => c.copy(coapsHost = x))
        .text("Set the secure local CoAP address. Default: localhost"),
      opt[Int]('p', "coapsport")
      .action((x,c) => c.copy(coapsPort = x))
      .text("Set the secure local CoAP port. Default: 5684"),
      help('h', "help")
        .text("Print the help message")
    )
  }

  logger.info("yo")

  OParser.parse(parser1, args, Config()) match {
    case Some(config) =>
      logger.info(config.toString)
      new Thread(new HopsKafkaProducer).start()
    case _ =>
  }

}

case class Config(
                 coapsHost: String = "localhost",
                 coapsPort: Int = LwM2m.DEFAULT_COAP_SECURE_PORT,
                 keyStorePath: Option[String] = None,
                 keyStoreType: Option[String] = None,
                 keyStorePass: Option[String] = None,
                 keyStoreAlias: Option[String] = None,
                 keyStoreAliasPass: Option[String] = None,
                 )
