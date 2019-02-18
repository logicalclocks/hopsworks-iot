package com.logicalclocks.example

import java.util
import java.util.Properties

import com.typesafe.config.ConfigFactory
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

  private val config = ConfigFactory.load("hops-kafka-consumer.properties")
  private val sslConfig = ConfigFactory.load("hops-kafka-ssl.properties")


  private val props: Properties = {
    val p = new Properties()
    p.put("bootstrap.servers", config.getString("bootstrap.servers"))
    p.put("key.deserializer", config.getString("key.deserializer"))
    p.put("value.deserializer", config.getString("value.deserializer"))
    p.put("security.protocol", config.getString("security.protocol"))
    p.put("group.id", config.getString("group.id"))
    p.put("auto.offset.reset", config.getString("auto.offset.reset"))
    p.put("consumer.timeout.ms", config.getString("consumer.timeout.ms"))
    p.put("auto.commit.interval.ms", config.getString("auto.commit.interval.ms"))

    p.put("ssl.truststore.location", sslConfig.getString("ssl.truststore.location"))
    p.put("ssl.truststore.password", sslConfig.getString("ssl.truststore.password"))
    p.put("ssl.keystore.location", sslConfig.getString("ssl.keystore.location"))
    p.put("ssl.keystore.password", sslConfig.getString("ssl.keystore.password"))
    p.put("ssl.key.password", sslConfig.getString("ssl.key.password"))
    p
  }

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
      userData.get("email").toString
    )
  }

  def run(): Unit = {
    logger.info("Start consumer")

    val consumer = new KafkaConsumer[String, Array[Byte]](props)
    Runtime.getRuntime.addShutdownHook(new Thread(() => consumer.close()))

    consumer.subscribe(util.Arrays.asList(config.getString("topic.name")))

    consumer.poll(0)
    consumer.seekToBeginning(consumer.assignment())
    while (true) {
      val records = consumer.poll(2000)
      records.forEach(record => {
        logger.info("Received a new message!!!")
        val user: User = getUser(record.value)
        logger.info(user.toString)
      }
      )
      consumer.commitSync
    }
  }


}

case class User(id: Int, name: String, email: String)
