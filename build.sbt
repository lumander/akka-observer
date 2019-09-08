name := "akka-observer"

version := "0.1"

scalaVersion := "2.12.8"


libraryDependencies += "com.lightbend.akka" %% "akka-stream-alpakka-file" % "1.0.0"
libraryDependencies += "com.lightbend.akka" %% "akka-stream-alpakka-csv" % "1.0.0"
libraryDependencies += "com.typesafe.akka" %% "akka-stream-kafka" % "1.0.3"
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.1.8"
libraryDependencies += "com.typesafe" % "config" % "1.2.1"
libraryDependencies += "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.7"
libraryDependencies += "com.lightbend.akka" %% "akka-stream-alpakka-json-streaming" % "1.0.0"
libraryDependencies += "com.typesafe.akka" %% "akka-slf4j" % "2.5.23"
libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3"