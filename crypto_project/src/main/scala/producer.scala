import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types._
import org.apache.spark.sql.functions._
import org.apache.spark.sql.streaming.Trigger


object CsvStreamingReader {

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("CSVStreamingReader")
      .master("local[*]")
      .getOrCreate()

    // Définir le schéma pour les données CSV
    val schema = StructType(Seq(
      StructField("Open", DoubleType, nullable = true),
      StructField("High", DoubleType, nullable = true),
      StructField("Low", DoubleType, nullable = true),
      StructField("Close", DoubleType, nullable = true),
      StructField("Volume", DoubleType, nullable = true)
    ))

    val inputDir = "streaming"

    val streamingInputDF = spark.readStream
      .option("header", "true")
      .option("inferSchema", "false") // Désactiver l'inférence de schéma automatique
      .schema(schema) // Appliquer le schéma défini
      .csv(inputDir)

    val processedDF = streamingInputDF
      .withColumnRenamed("Open", "open_price")  // Exemple de transformation simple

    // Requête de sortie en mode append avec une fréquence de traitement de 5 secondes
    val query = processedDF.writeStream
      .outputMode("append")  // Mode d'addition des nouvelles lignes
      .format("console")     // Affichage dans la console
      // .trigger(Trigger.ProcessingTime("5 seconds"))  // Déclenchement tous les 5 secondes
      .start()

    query.awaitTermination()
  }
}
