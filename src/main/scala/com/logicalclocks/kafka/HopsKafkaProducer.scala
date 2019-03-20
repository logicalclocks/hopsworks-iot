package com.logicalclocks.kafka

import java.io.ByteArrayOutputStream
import java.util.Properties

import com.logicalclocks.commons.PropertiesReader
import com.logicalclocks.lwm2m.IpsoObjectMeasurement
import com.logicalclocks.lwm2m.TempIpsoObjectMeasurement
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io.BinaryEncoder
import org.apache.avro.io.EncoderFactory
import org.apache.avro.specific.SpecificDatumWriter
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.util.Failure
import scala.util.Success
import scala.util.Try

case class HopsKafkaProducer() {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  private val props: Properties = PropertiesReader()
    .addResource("hops-kafka-producer.conf", "kafka")
    .addResource("hops-kafka-ssl.conf", "kafka")
    .props

  private val producer = new KafkaProducer[String, Array[Byte]](props)

  def close(): Unit =
    producer.close()

  def sendIpsoObject(obj: IpsoObjectMeasurement, schemaOption: Option[Schema]) = {
    if (schemaOption.nonEmpty) {
      Try(getGenericRecord(obj, schemaOption.get))
        .map(serializeRecord(_, schemaOption.get))
        .map(new ProducerRecord[String, Array[Byte]]("TempMeasurementsTopic", obj.endpointClientName, _)) match {
          case Success(s) => producer.send(s)
          case Failure(f) => logger.error("error trying to send to kafka: " + f)
        }
    } else {
      logger.warn("No corresponding avro schema for received object of class" + obj.getClass)

    }
  }

  private def getGenericRecord(obj: IpsoObjectMeasurement, schema: Schema): GenericRecord = obj match {
    case t: TempIpsoObjectMeasurement => getGenericRecordTemp(t, schema)
    case _ => throw new Error("Trying to send unknown ipso object to Kafka")
  }

  private def getGenericRecordTemp(measurement: TempIpsoObjectMeasurement, schema: Schema): GenericRecord = {
    val record = new GenericData.Record(schema)
    record.put("timestamp", measurement.timestamp)
    record.put("endpointClientName", measurement.endpointClientName)
    record.put("sensorValue", measurement.sensorValue)
    measurement.minMeasuredValue.foreach(record.put("minMeasuredValue", _))
    measurement.maxMeasuredValue.foreach(record.put("maxMeasuredValue", _))
    measurement.minRangeValue.foreach(record.put("minRangeValue", _))
    measurement.maxRangeValue.foreach(record.put("maxRangeValue", _))
    measurement.sensorUnits.foreach(record.put("sensorUnits", _))
    measurement.resetMinAndMaxMeasuredValues.foreach(record.put("resetMinAndMaxMeasuredValues", _))
    record
  }

  private def serializeRecord(record: GenericRecord, schema: Schema): Array[Byte] = {
    val writer = new SpecificDatumWriter[GenericRecord](schema)
    val out = new ByteArrayOutputStream()
    val encoder: BinaryEncoder = EncoderFactory.get().binaryEncoder(out, null)
    writer.write(record, encoder)
    encoder.flush()
    out.close()
    out.toByteArray
  }
}
