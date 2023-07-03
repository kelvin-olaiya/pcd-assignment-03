ThisBuild / scalaVersion := "3.3.0"
val AkkaVersion = "2.8.2"

libraryDependencies ++= Seq(
    "com.typesafe.akka" %% "akka-actor-typed" % AkkaVersion,
    "com.typesafe.akka" %% "akka-actor-testkit-typed" % AkkaVersion % Test,
    "ch.qos.logback" % "logback-classic" % "1.1.3" % Runtime
)