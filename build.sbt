ThisBuild / scalaVersion := "3.3.0"
val AkkaVersion = "2.8.2"

libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
    "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
    "com.typesafe.akka" %% "akka-stream" % AkkaVersion,
    "ch.qos.logback" % "logback-classic" % "1.1.3" % Runtime,
    "com.rabbitmq" % "amqp-client" % "5.18.0",
    "com.google.code.gson" % "gson" % "2.10.1"
)