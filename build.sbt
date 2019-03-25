
scalaVersion := "2.12.8"
version := "0.1.0-SNAPSHOT"
organization := "com.logicalclocks"
organizationName := "Logical Clocks AB"

name := "Hops IoT Gateway"

addCommandAlias("testc", ";clean;coverage;test;coverageReport")

lazy val akkaVersion = "2.5.21"
lazy val avroVersion = "1.8.2"
lazy val catsVersion = "1.6.0"
lazy val jettyVersion = "9.4.15.v20190215"
lazy val kafkaVersion = "1.1.0"
lazy val leshanVersion = "1.0.0-M10"
lazy val logbackVersion = "1.1.2"
lazy val scalaLoggingVersion = "3.9.2"
lazy val scalaTestVersion = "3.0.5"
lazy val scoptVersion = "4.0.0-RC2"
lazy val avro4sVersion = "2.0.4"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % logbackVersion,
  "com.github.scopt" %% "scopt" % scoptVersion,
  "com.sksamuel.avro4s" %% "avro4s-kafka" % avro4sVersion,
  "com.typesafe" % "config" % "1.3.3",
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion,
  "org.apache.avro" % "avro" % avroVersion,
  "org.apache.kafka" % "kafka-clients" % kafkaVersion,
  "org.eclipse.leshan" % "leshan-server-cf" % leshanVersion,
  "org.eclipse.leshan" % "leshan-server-demo" % leshanVersion,
  "org.eclipse.jetty" % "jetty-webapp" % jettyVersion,
  "org.eclipse.jetty" % "jetty-continuation" % jettyVersion,
  "org.typelevel" %% "cats-core" % catsVersion
)

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test
)

mainClass in assembly := Some("com.logicalclocks.IotGateway")
