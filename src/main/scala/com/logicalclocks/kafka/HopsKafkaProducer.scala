package com.logicalclocks.kafka

import java.io.ByteArrayOutputStream
import java.util.Properties

import com.logicalclocks.commons.PropertiesReader
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

  private val props: Properties = PropertiesReader()
    .addResource("hops-kafka-producer.conf", "kafka")
    .addResource("hops-kafka-ssl.conf", "kafka")
    .props

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
    val queueMessage = new ProducerRecord[String, Array[Byte]](props.getProperty("topic.name"), serializedBytes)

    producer.send(queueMessage)
    logger.info("Sent user to topic")
    producer.close

  }
}

object HopsKafkaProducer {

}
