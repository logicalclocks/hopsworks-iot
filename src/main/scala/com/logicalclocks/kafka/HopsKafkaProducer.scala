package com.logicalclocks.kafka

import java.io.ByteArrayOutputStream
import java.util.Properties

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.Logger
import org.apache.avro.Schema
import org.apache.avro.Schema.Parser
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io.BinaryEncoder
import org.apache.avro.io.EncoderFactory
import org.apache.avro.specific.SpecificDatumWriter
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

import scala.io.Source

class HopsKafkaProducer extends Runnable {

  private val logger = Logger[HopsKafkaProducer]

  private val config = ConfigFactory.load("hops-kafka-producer.properties")
  private val sslConfig = ConfigFactory.load("hops-kafka-ssl.properties")

  private val props: Properties = {
    val p = new Properties()
    p.put("bootstrap.servers", config.getString("bootstrap.servers"))
    p.put("key.serializer", config.getString("key.serializer"))
    p.put("value.serializer", config.getString("value.serializer"))
    p.put("acks", config.getString("acks"))
    p.put("retries", config.getString("retries"))
    p.put("linger.ms", config.getString("linger.ms"))
    p.put("security.protocol", config.getString("security.protocol"))
    p.put("ssl.truststore.location", sslConfig.getString("ssl.truststore.location"))
    p.put("ssl.truststore.password", sslConfig.getString("ssl.truststore.password"))
    p.put("ssl.keystore.location", sslConfig.getString("ssl.keystore.location"))
    p.put("ssl.keystore.password", sslConfig.getString("ssl.keystore.password"))
    p.put("ssl.key.password", sslConfig.getString("ssl.key.password"))
    p
  }

  private val producer = new KafkaProducer[String, Array[Byte]](props)

  def run(): Unit = {
    val schema: Schema = new Parser().parse(Source.fromURL(getClass.getResource("/avro/sample.avsc")).mkString)
    val genericUser: GenericRecord = new GenericData.Record(schema)
    genericUser.put("id", 1)
    genericUser.put("name", "kimchi")
    genericUser.put("email", "his@mail.com")

    val writer = new SpecificDatumWriter[GenericRecord](schema)
    val out = new ByteArrayOutputStream()
    val encoder: BinaryEncoder = EncoderFactory.get().binaryEncoder(out, null)
    writer.write(genericUser, encoder)
    encoder.flush()
    out.close()

    val serializedBytes: Array[Byte] = out.toByteArray
    val queueMessage = new ProducerRecord[String, Array[Byte]](config.getString("topic.name"), serializedBytes)

    producer.send(queueMessage)
    logger.info("Sent user to topic")
    producer.close

  }
}

object HopsKafkaProducer {

}
