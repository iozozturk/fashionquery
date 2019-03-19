name := "fashionquery"

version := "0.1"

scalaVersion := "2.12.8"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % "10.1.7",
  "com.typesafe.akka" %% "akka-stream-kafka" % "1.0.1",
  "com.typesafe.akka" %% "akka-slf4j" % "2.5.21",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.elasticsearch.client" % "transport" % "6.6.2",
  "com.typesafe.play" %% "play-json" % "2.7.2"
)
