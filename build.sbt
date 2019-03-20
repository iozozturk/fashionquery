
name := "fashionquery"

version := "0.1"

scalaVersion := "2.12.8"

val akkaVersion = "2.5.21"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % "10.1.7",
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream-kafka" % "1.0.1",
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.elasticsearch.client" % "transport" % "6.6.2",
  "com.typesafe.play" %% "play-json" % "2.7.2",
  "de.heikoseeberger" %% "akka-http-play-json" % "1.25.2",
  "org.scalatest" %% "scalatest" % "3.0.6" % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
  "org.mockito" % "mockito-core" % "2.25.0" % Test
)
