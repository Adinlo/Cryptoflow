ThisBuild / scalaVersion := "2.12.18"
ThisBuild / version := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .settings(
    name := "Cryptoflow",
    version := "0.1",
    libraryDependencies ++= Seq(
      "org.apache.spark" %% "spark-core" % "3.5.3",
      "org.apache.spark" %% "spark-sql" % "3.5.3",
      "org.apache.spark" %% "spark-streaming" % "3.5.3",
      "org.apache.spark" %% "spark-sql-kafka-0-10" % "3.5.3",
      "org.apache.hadoop" % "hadoop-common" % "3.3.1",
      "org.apache.hadoop" % "hadoop-hdfs" % "3.3.1",
      "org.apache.kafka" % "kafka-clients" % "3.3.1",
      "org.apache.spark" %% "spark-sql-kafka-0-10" % "3.2.0",

    ),
   //Compile / run / mainClass := Some("CsvStreamingReader.main")   // change la mainclass 
  )