import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.sql.types._

object KafkaToCsvConsumer {
  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("KafkaToCsvConsumer")
      .master("local[*]")
      .getOrCreate()

    // Import nécessaire pour les encoders implicites
    import spark.implicits._

    // Définir le schéma des données
    val schema = StructType(Seq(
      StructField("Open_time", StringType, nullable = true),
      StructField("Open", DoubleType, nullable = true),
      StructField("High", DoubleType, nullable = true),
      StructField("Low", DoubleType, nullable = true),
      StructField("Close", DoubleType, nullable = true),
      StructField("Volume", DoubleType, nullable = true)
    ))

    // Lire les messages de Kafka
    val kafkaStreamDF = spark.readStream
      .format("kafka")
      .option("kafka.bootstrap.servers", "kafka:9092") // Adresse Kafka dans Docker
      .option("subscribe", "btc_topic") // Nom du topic Kafka
      .load()

    // Extraire les valeurs des messages Kafka
    val valueDF = kafkaStreamDF.selectExpr("CAST(value AS STRING)").as[String]

    // Transformer les messages en colonnes
    val processedDF = valueDF
      .map(row => row.split(";"))
      .map(parts => (parts(0), parts(1).toDouble, parts(2).toDouble, parts(3).toDouble, parts(4).toDouble, parts(5).toDouble))
      .toDF("Open_time", "Open", "High", "Low", "Close", "Volume")
      .withColumn("Open_time", to_timestamp(col("Open_time"), "dd/MM/yyyy HH:mm")) // Adapter le format de la date

    // Calcul des statistiques en utilisant une fenêtre
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

    // Écrire les résultats dans un fichier CSV
    val query = windowedDF.writeStream
      .outputMode("append")
      .format("csv")
      .option("path", "output/results/") // Chemin des fichiers de sortie
      .option("checkpointLocation", "output/checkpoint/") // Chemin pour le checkpoint
      .start()

    query.awaitTermination()
  }
}
