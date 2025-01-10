// import org.apache.spark.sql.SparkSession
// import org.apache.spark.sql.functions._
// import org.apache.spark.sql.types._
// import org.apache.spark.sql.streaming.Trigger

// object KafkaToCsvConsumer {
//   def main(args: Array[String]): Unit = {
//     val spark = SparkSession.builder()
//       .appName("KafkaToCsvConsumer")
//       .master("local[*]")
//       .getOrCreate()

//     import spark.implicits._

//     // Schéma des données
//     val schema = StructType(Seq(
//       StructField("Open_time", StringType, nullable = false),
//       StructField("Open", DoubleType, nullable = false),
//       StructField("High", DoubleType, nullable = false),
//       StructField("Low", DoubleType, nullable = false),
//       StructField("Close", DoubleType, nullable = false),
//       StructField("Volume", DoubleType, nullable = false)
//     ))

//     // Lecture des données depuis Kafka
//     val kafkaStreamDF = spark.readStream
//       .format("kafka")
//       .option("kafka.bootstrap.servers", "localhost:9092")
//       .option("subscribe", "btc_topic_1")
//       .load()

//     // Conversion des valeurs Kafka en String
//     val valueDF = kafkaStreamDF.selectExpr("CAST(value AS STRING)").as[String]

//     // Parsing des données et gestion des erreurs
//     val processedDF = valueDF
//       .map(row => {
//         try {
//           val parts = row.split(";")
//           if (parts.length == 6) {
//             Some((parts(0), parts(1).toDouble, parts(2).toDouble, parts(3).toDouble, parts(4).toDouble, parts(5).toDouble))
//           } else {
//             None
//           }
//         } catch {
//           case e: Exception =>
//             // Log des erreurs de parsing (à implémenter avec un système de journalisation)
//             println(s"Erreur de parsing pour la ligne : $row. Erreur : ${e.getMessage}")
//             None
//         }
//       })
//       .filter(_.isDefined) // Filtre les lignes mal formatées
//       .map(_.get) // Extrait les valeurs Option
//       .toDF("Open_time", "Open", "High", "Low", "Close", "Volume")
//       .withColumn("Open_time", to_timestamp(col("Open_time"), "dd/MM/yyyy HH:mm")) // Conversion en timestamp

//     // Agrégations par fenêtre de 4 heures
//     val windowedDF = processedDF
//       .withWatermark("Open_time", "2 hours") // Watermark de 2 heures
//       .groupBy(
//         window(col("Open_time"), "4 hours") // Fenêtre de 4 heures
//       )
//       .agg(
//         avg("Close").alias("avg_close"),
//         stddev("Close").alias("stddev_close"),
//         min("Close").alias("min_close"),
//         max("Close").alias("max_close")
//       )

//     // Aplatissement de la fenêtre
//     val flattenedDF = windowedDF
//       .withColumn("window_start", col("window.start")) // Début de la fenêtre
//       .withColumn("window_end", col("window.end")) // Fin de la fenêtre
//       .drop("window") // Suppression de la colonne window

//     // Écriture des résultats en CSV
//     val query = flattenedDF.writeStream
//       .outputMode("append") // Mode append pour les agrégations avec watermark
//       .format("csv")
//       .option("path", "output/results") // Répertoire de sortie
//       .option("checkpointLocation", "output/checkpoint/") // Activation du checkpointing
//       .trigger(Trigger.ProcessingTime("1 minute")) // Déclenchement toutes les 1 minute
//       .start()

//     query.awaitTermination()
//   }
// }

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._
import org.apache.spark.sql.streaming.Trigger

object KafkaToCsvConsumer {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("KafkaToCsvConsumer")
      .master("local[*]")
      .getOrCreate()

    import spark.implicits._

    val schema = StructType(Seq(
      StructField("Open_time", StringType, nullable = false),
      StructField("Open", DoubleType, nullable = false),
      StructField("High", DoubleType, nullable = false),
      StructField("Low", DoubleType, nullable = false),
      StructField("Close", DoubleType, nullable = false),
      StructField("Volume", DoubleType, nullable = false)
    ))

    val kafkaStreamDF = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "localhost:9092")
      .option("subscribe", "btc_topic_1")
      .load()

    val valueDF = kafkaStreamDF.selectExpr("CAST(value AS STRING)").as[String]

    val processedDF = valueDF
      .map(row => {
        try {
          val parts = row.split(",")
          if (parts.length == 6) {
            Some((parts(0), parts(1).toDouble, parts(2).toDouble, parts(3).toDouble, parts(4).toDouble, parts(5).toDouble))
          } else {
            None
          }
        } catch {
          case e: Exception =>
            println(s"Erreur de parsing pour la ligne : $row. Erreur : ${e.getMessage}")
            None
        }
      })
      .filter(_.isDefined) 
      .map(_.get) 
      .toDF("Open_time", "Open", "High", "Low", "Close", "Volume")
      .withColumn("Open_time", to_timestamp(col("Open_time"), "dd/MM/yyyy HH:mm")) 

    val windowedDF = processedDF
      .withWatermark("Open_time", "2 hours") 
      .groupBy(
        window(col("Open_time"), "4 hours") 
      )
      .agg(
        avg("Close").alias("avg_close"),
        stddev("Close").alias("stddev_close"),
        min("Close").alias("min_close"),
        max("Close").alias("max_close")
      )

    // Aplatissement de la fenêtre
    val flattenedDF = windowedDF
      .withColumn("window_start", col("window.start")) // Début de la fenêtre
      .withColumn("window_end", col("window.end")) // Fin de la fenêtre
      .drop("window") // Suppression de la colonne window

    // Configuration de la connexion à PostgreSQL
    val jdbcUrl = "jdbc:postgresql://localhost:5432/Btc_db"
    val connectionProperties = new java.util.Properties()
    connectionProperties.put("user", "postgres")
    connectionProperties.put("password", "1234")
    connectionProperties.put("driver", "org.postgresql.Driver")

    val query = flattenedDF.writeStream
      .outputMode("append") // Mode append pour les agrégations avec watermark
      .foreachBatch { (batchDF: org.apache.spark.sql.DataFrame, batchId: Long) =>
        batchDF.write
          .mode("append")
          .jdbc(jdbcUrl, "btc_data", connectionProperties)
      }
      .option("checkpointLocation", "output/checkpoint/") /
      .trigger(Trigger.ProcessingTime("1 minute")) 
      .start()

    query.awaitTermination()
  }
}