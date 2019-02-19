package com.logicalclocks.example

import java.util
import java.util.Properties

import com.logicalclocks.commons.PropertiesReader
import com.typesafe.scalalogging.Logger
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io.DatumReader
import org.apache.avro.io.Decoder
import org.apache.avro.io.DecoderFactory
import org.apache.avro.specific.SpecificDatumReader
import org.apache.kafka.clients.consumer.KafkaConsumer

import scala.io.Source

class HopsKafkaConsumer extends Runnable {

  private val logger = Logger[HopsKafkaConsumer]

  private val props: Properties = PropertiesReader()
    .addResource("hops-kafka-consumer.conf", "kafka")
    .addResource("hops-kafka-ssl.conf", "kafka")
    .props

  val schemaString = Source.fromURL(getClass.getResource("/avro/sample.avsc")).mkString
  // Initialize schema
  val schema: Schema = new Schema.Parser().parse(schemaString)

  private def getUser(message: Array[Byte]): User = {
    val reader: DatumReader[GenericRecord] = new SpecificDatumReader[GenericRecord](schema)
    val decoder: Decoder = DecoderFactory.get.binaryDecoder(message, null)
    val userData: GenericRecord = reader.read(null, decoder)

    User(
      userData.get("id").toString.toInt,
      userData.get("name").toString,
      userData.get("email").toString)
  }

  def run(): Unit = {
    logger.info("*" * 60)
    logger.info("Start consumer")

    val consumer = new KafkaConsumer[String, Array[Byte]](props)
    Runtime.getRuntime.addShutdownHook(new Thread(() => consumer.close()))

    consumer.subscribe(util.Arrays.asList(props.getProperty("topic.name")))

    consumer.poll(0)
    consumer.seekToBeginning(consumer.assignment())
    while (true) {
      val records = consumer.poll(2000)
      records.forEach(record => {
        logger.info("Received a new message!!!")
        val user: User = getUser(record.value)
        logger.info(user.toString)
      })
      consumer.commitSync
    }
  }

}

case class User(id: Int, name: String, email: String)
