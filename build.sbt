
scalaVersion := "2.12.8"
version := "0.1.0-SNAPSHOT"
organization := "com.logicalclocks"
organizationName := "Logical Clocks AB"

name := "Hops IoT Edge Server"

addCommandAlias("testc", ";clear;coverage;test;coverageReport")

libraryDependencies ++= Seq(
  "org.eclipse.leshan" % "leshan-server-cf" % "1.0.0-M10",
  "com.github.scopt" %% "scopt" % "4.0.0-RC2",
  "org.apache.avro" % "avro" % "1.8.2",
  "com.typesafe" % "config" % "1.3.3",
  "org.apache.kafka" % "kafka-clients" % "1.1.0",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  "ch.qos.logback" % "logback-classic" % "1.1.2",
  "org.scalatest" %% "scalatest" % "3.0.5" % Test
)
