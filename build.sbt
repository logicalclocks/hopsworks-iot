
scalaVersion := "2.12.8"
version := "0.1.0-SNAPSHOT"
organization := "com.logicalclocks"
organizationName := "Logical Clocks AB"

name := "Hops IoT Gateway"

mainClass in assembly := Some("com.logicalclocks.iot.IotGateway")

addCommandAlias("testc", ";clean;coverage;test;coverageReport")

scalacOptions ++= Seq(
  "-deprecation",
  "-feature")

parallelExecution in Test := false

logBuffered := false

fork in run := true

testOptions += Tests.Argument(TestFrameworks.JUnit, "-q", "-v", "-s", "-a")

test in assembly := {}

lazy val akkaVersion = "2.5.21"
lazy val akkaHttpVersion = "10.1.8"
lazy val avro4sVersion = "2.0.4"
lazy val avroVersion = "1.8.2"
lazy val catsCoreVersion = "1.6.0"
lazy val catsEffectVersion = "1.2.0"
lazy val commonsNetVersion = "3.1"
lazy val h2Version = "1.4.199"
lazy val jettyVersion = "9.4.15.v20190215"
lazy val junitInterfaceVersion = "0.11"
lazy val kafkaVersion = "1.1.0"
lazy val leshanVersion = "1.0.0-M10"
lazy val logbackVersion = "1.1.2"
lazy val neo4jVersion = "3.5.4"
lazy val scalaLoggingVersion = "3.9.2"
lazy val scalaTestVersion = "3.0.5"
lazy val slickHickariCpVersion = "3.3.0"
lazy val slickVersion = "3.3.0"
lazy val slickTestKitVersion = "3.2.3"
lazy val typesafeConfigVersion = "1.3.3"

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % logbackVersion,
  "com.h2database" % "h2" % h2Version,
  "com.sksamuel.avro4s" %% "avro4s-kafka" % avro4sVersion,
  "com.typesafe" % "config" % typesafeConfigVersion,
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe" % "config" % typesafeConfigVersion,
  "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion,
  "org.apache.avro" % "avro" % avroVersion,
  "org.apache.kafka" % "kafka-clients" % kafkaVersion,
  "org.eclipse.leshan" % "leshan-server-cf" % leshanVersion,
  "org.eclipse.leshan" % "leshan-server-demo" % leshanVersion,
  "org.eclipse.jetty" % "jetty-webapp" % jettyVersion,
  "org.eclipse.jetty" % "jetty-continuation" % jettyVersion,
  "org.typelevel" %% "cats-core" % catsCoreVersion,
  "org.typelevel" %% "cats-effect" % catsEffectVersion,
  "commons-net" % "commons-net" % commonsNetVersion,

  "com.typesafe.slick" %% "slick" % slickVersion,
  "com.typesafe.slick" %% "slick-hikaricp" % slickHickariCpVersion
)

libraryDependencies ++= Seq(
  "com.novocode" % "junit-interface" % junitInterfaceVersion % Test,
  "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
  "com.typesafe.slick" %% "slick-testkit" % slickTestKitVersion % Test
)
