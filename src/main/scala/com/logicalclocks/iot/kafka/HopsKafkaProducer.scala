package com.logicalclocks.iot.kafka

import java.io.ByteArrayOutputStream
import java.util.Properties

import com.logicalclocks.iot.commons.PropertiesReader
import com.logicalclocks.iot.lwm2m.IpsoObjectMeasurement
import com.logicalclocks.iot.lwm2m.TempIpsoObjectMeasurement
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord
import org.apache.avro.io.BinaryEncoder
import org.apache.avro.io.EncoderFactory
import org.apache.avro.specific.SpecificDatumWriter
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.config.SslConfigs
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.util.Failure
import scala.util.Success
import scala.util.Try

case class HopsKafkaProducer(kStorePath: String, tStorePath: String, pass: String) {

  val logger: Logger = LoggerFactory.getLogger(getClass)

  private val props: Properties = {
    val p = PropertiesReader()
      .addResource("hops-kafka-producer.conf", "kafka")
      .props
    p.put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, tStorePath)
    p.put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, kStorePath)
    p.put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, pass)
    p.put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, pass)
    p.put(SslConfigs.SSL_KEY_PASSWORD_CONFIG, pass)
    p.put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "")
    p
  }

  private val producer = new KafkaProducer[String, Array[Byte]](props)

  def close(): Unit =
    producer.close()

  def sendIpsoObject(obj: IpsoObjectMeasurement, schemaOption: Option[Schema]) = {
    val topic: Option[String] = LwM2mTopics.findNameByObjectId(obj.objectId)
    if (topic.isEmpty) {
      logger.error(s"Cannot find topic for objectId ${obj.objectId}.")
    } else if (schemaOption.nonEmpty) {
      Try(getGenericRecord(obj, schemaOption.get))
        .map(serializeRecord(_, schemaOption.get))
        .map(new ProducerRecord[String, Array[Byte]](topic.get, obj.endpointClientName, _)) match {
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
    record.put("instanceId", measurement.instanceId)
    val ipsoObject = new GenericData.Record(schema.getField("ipsoObject").schema)
    ipsoObject.put("sensorValue", measurement.ipsoObject.sensorValue)
    measurement.ipsoObject.minMeasuredValue.foreach(ipsoObject.put("minMeasuredValue", _))
    measurement.ipsoObject.maxMeasuredValue.foreach(ipsoObject.put("maxMeasuredValue", _))
    measurement.ipsoObject.minRangeValue.foreach(ipsoObject.put("minRangeValue", _))
    measurement.ipsoObject.maxRangeValue.foreach(ipsoObject.put("maxRangeValue", _))
    measurement.ipsoObject.sensorUnits.foreach(ipsoObject.put("sensorUnits", _))
    measurement.ipsoObject.resetMinAndMaxMeasuredValues.foreach(ipsoObject.put("resetMinAndMaxMeasuredValues", _))
    record.put("ipsoObject", ipsoObject)
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
