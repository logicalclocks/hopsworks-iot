
scalaVersion := "2.12.8"
version := "0.1.0-SNAPSHOT"
organization := "com.logicalclocks"
organizationName := "Logical Clocks AB"

name := "Hops IoT Gateway"

addCommandAlias("testc", ";clean;coverage;test;coverageReport")

lazy val akkaVersion = "2.5.21"
lazy val jettyVersion = "9.4.15.v20190215"

libraryDependencies ++= Seq(
  "org.eclipse.leshan" % "leshan-server-cf" % "1.0.0-M10",
  "org.eclipse.leshan" % "leshan-server-demo" % "1.0.0-M10",
  "com.github.scopt" %% "scopt" % "4.0.0-RC2",
  "org.apache.avro" % "avro" % "1.8.2",
  "com.typesafe" % "config" % "1.3.3",
  "org.apache.kafka" % "kafka-clients" % "1.1.0",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.2",
  "ch.qos.logback" % "logback-classic" % "1.1.2",
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "org.eclipse.jetty" % "jetty-webapp" % jettyVersion,
  "org.eclipse.jetty" % "jetty-continuation" % jettyVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "org.scalatest" %% "scalatest" % "3.0.5" % Test
)
