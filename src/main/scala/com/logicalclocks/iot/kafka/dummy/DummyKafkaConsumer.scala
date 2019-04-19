package com.logicalclocks.iot.kafka.dummy

import java.util
import java.util.Properties

import com.logicalclocks.iot.commons.PropertiesReader
import com.logicalclocks.iot.lwm2m.TempIpsoObject
import com.logicalclocks.iot.lwm2m.TempIpsoObjectMeasurement
import com.typesafe.scalalogging.Logger
import org.apache.avro.Schema
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io.DatumReader
import org.apache.avro.io.Decoder
import org.apache.avro.io.DecoderFactory
import org.apache.avro.specific.SpecificDatumReader
import org.apache.kafka.clients.consumer.KafkaConsumer

import scala.io.Source

object DummyConsumerApp extends App {
  new DummyKafkaConsumer().run()
}

class DummyKafkaConsumer extends Runnable {

  private val logger = Logger[DummyKafkaConsumer]

  private val props: Properties = PropertiesReader()
    .addResource("hops-kafka-consumer.conf", "kafka")
    .addResource("hops-kafka-ssl.conf", "kafka")
    .props

  val schemaString = Source.fromURL(getClass.getResource("/avro/lwm2m/3303.avsc")).mkString
  // Initialize schema
  val schema: Schema = new Schema.Parser().parse(schemaString)

  private def getUser(message: Array[Byte]): TempIpsoObjectMeasurement = {
    val reader: DatumReader[GenericRecord] = new SpecificDatumReader[GenericRecord](schema)
    val decoder: Decoder = DecoderFactory.get.binaryDecoder(message, null)
    val userData: GenericRecord = reader.read(null, decoder)

    val ipsoObjectStruct = userData.get("ipsoObject").asInstanceOf[GenericRecord]
    val minMeasuredValue = Option(ipsoObjectStruct.get("minMeasuredValue")).map(_.asInstanceOf[Double])
    val maxMeasuredValue = Option(ipsoObjectStruct.get("maxMeasuredValue")).map(_.asInstanceOf[Double])
    val minRangeValue = Option(ipsoObjectStruct.get("minRangeValue")).map(_.asInstanceOf[Double])
    val maxRangeValue = Option(ipsoObjectStruct.get("maxRangeValue")).map(_.asInstanceOf[Double])
    val sensorUnits = Option(ipsoObjectStruct.get("sensorUnits")).map(_.toString)
    val resetMinAndMaxMeasuredValues = Option(ipsoObjectStruct.get("resetMinAndMaxMeasuredValues")).map(_.asInstanceOf[Boolean])

    val ipsoObject = TempIpsoObject(
      ipsoObjectStruct.get("sensorValue").toString.toDouble,
      minMeasuredValue,
      maxMeasuredValue,
      minRangeValue,
      maxRangeValue,
      sensorUnits,
      resetMinAndMaxMeasuredValues)

    TempIpsoObjectMeasurement(
      userData.get("timestamp").toString.toLong,
      userData.get("endpointClientName").toString,
      userData.get("instanceId").toString.toInt,
      ipsoObject)
  }

  def run(): Unit = {
    logger.info("*" * 60)
    logger.info("Start consumer")

    val consumer = new KafkaConsumer[String, Array[Byte]](props)
    Runtime.getRuntime.addShutdownHook(new Thread(() => consumer.close()))

    consumer.subscribe(util.Arrays.asList("topic-lwm2m-3303-temperature"))

    consumer.poll(0)
    consumer.seekToBeginning(consumer.assignment())
    while (true) {
      val records = consumer.poll(2000)
      records.forEach(record => {
        val measurement: TempIpsoObjectMeasurement = getUser(record.value)
        logger.info("!!! " + measurement.toString)
      })
      consumer.commitSync
    }
  }

}

case class User(id: Int, name: String, email: String)

