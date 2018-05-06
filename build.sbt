import Dependencies._

lazy val root = (project in file(".")).
  settings(
    inThisBuild(List(
      organization := "com.example",
      scalaVersion := "2.12.6",
      version      := "0.1.0-SNAPSHOT"
    )),
    name := "IoTClient",
    libraryDependencies ++= Seq(scalaTest % Test,
     "com.microsoft.azure.sdk.iot" % "iot-device-client" % "1.10.0",
     "com.microsoft.azure" % "azure-eventhubs" % "1.0.1",
     "com.typesafe.play" %% "play-json" % "2.6.9"
    )
  )

mainClass in assembly := Some("example.Hello")
javacOptions ++= Seq("-source", "1.8", "-target", "1.8", "-Xlint")
