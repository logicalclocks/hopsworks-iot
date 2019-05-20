package com.logicalclocks.iot.kafka

import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

import cats.effect.IO
import cats.effect.Resource
import com.logicalclocks.iot.hopsworks.GatewayCertsDTO
import com.typesafe.config.ConfigFactory
import org.apache.commons.net.util.Base64
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import scala.reflect.io.Directory

case class HopsFileWriter() {
  val folder = ConfigFactory.load.getString("gateway.directory") + "/certs"

  private val logger: Logger = LoggerFactory.getLogger(getClass)
  private val folderFile = new File(folder)

  def createFolder(): IO[Boolean] =
    IO.pure(folderFile.mkdirs())

  def saveCertsToFiles(certsDTO: GatewayCertsDTO): IO[(String, String)] =
    for {
      kPath <- saveCertStringToFile(certsDTO.kStore, "keystore", certsDTO.fileExtension)
      tPath <- saveCertStringToFile(certsDTO.tStore, "truststore", certsDTO.fileExtension)
    } yield (kPath, tPath)

  def cleanUp(): IO[Boolean] =
    IO.pure(new Directory(folderFile).deleteRecursively() && folderFile.delete())

  private def saveCertStringToFile(encodedString: String, fileName: String, fileExtension: String): IO[String] =
    for {
      _ <- IO.unit
      buffer = Base64.decodeBase64(encodedString)
      path = folder + "/" + fileName + "." + fileExtension
      file = new File(path)
      count <- write(buffer, file)
      _ <- IO(logger.debug(s"Copied $count bytes to $path"))
    } yield path

  private def write(bytes: Array[Byte], destination: File): IO[Long] =
    outputStream(destination).use(transfer(bytes, _))

  private def outputStream(f: File): Resource[IO, FileOutputStream] =
    Resource.fromAutoCloseable(IO(new FileOutputStream(f)))

  private def transfer(bytes: Array[Byte], destination: OutputStream): IO[Long] =
    for {
      _ <- IO(destination.write(bytes))
      count <- IO.pure(bytes.length)
    } yield count
}
