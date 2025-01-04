// import scala.io.Source
// import java.io.{File, PrintWriter}


// // READSTREAM
// ///

// object CsvSplitter {

//   def main(args: Array[String]): Unit = {
//     val singleCsvPath = "src/resources/btc_4h_data_2018_to_2024-2024-12-10(1) (1).csv"
//     val outputDir = "streaming"

//    /* val outputDirectory = new File(outputDir)
//     if (outputDirectory.exists && outputDirectory.isDirectory) {
//       outputDirectory.listFiles().foreach(_.delete())
//     }*/

//     val lines = Source.fromFile(singleCsvPath).getLines().toList

//     val header = lines.head
//     val dataLines = lines.tail

//     val chunkSize = 10
//     var chunkIndex = 0

//     while (chunkIndex * chunkSize < dataLines.length) {
//       val chunk = dataLines.slice(chunkIndex * chunkSize, (chunkIndex + 1) * chunkSize)

//       val chunkFileName = s"$outputDir/chunk$chunkIndex.csv"
//       val writer = new PrintWriter(chunkFileName)

//       writer.println(header)
//       chunk.foreach(writer.println)

//       writer.close()

//       // println(s"Wrote $chunkFileName with ${chunk.size} lines.")
//       Thread.sleep(5000) 
//       chunkIndex += 1
//     }

//     println("Done splitting CSV into 10-line chunks.")
//   }
// }


import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.streaming.Trigger
import scala.io.Source
import java.io.{File, PrintWriter}

object CsvSplitter {

  def main(args: Array[String]): Unit = {
    val singleCsvPath = "src/resources/btc_4h_data_2018_to_2024-2024-12-10(1) (1).csv"
    val outputDir = "streaming"

    val lines = Source.fromFile(singleCsvPath).getLines().toList
    val header = lines.head
    val dataLines = lines.tail

    val chunkSize = 10
    var chunkIndex = 0

    // Vérifiez et nettoyez le dossier de sortie
    val outputDirectory = new File(outputDir)
    if (outputDirectory.exists && outputDirectory.isDirectory) {
      outputDirectory.listFiles().foreach(_.delete())
    } else {
      outputDirectory.mkdirs()
    }

    // Créer des chunks en continu
    new Thread(() => {
      while (chunkIndex * chunkSize < dataLines.length) {
        val chunk = dataLines.slice(chunkIndex * chunkSize, (chunkIndex + 1) * chunkSize)
        val chunkFileName = s"$outputDir/chunk$chunkIndex.csv"
        val writer = new PrintWriter(chunkFileName)

        writer.println(header)
        chunk.foreach(writer.println)

        writer.close()
        println(s"Wrote $chunkFileName with ${chunk.size} lines.")
        Thread.sleep(5000) // Simule une arrivée de fichier toutes les 5 secondes
        chunkIndex += 1
      }
    }).start()

    // Initialisation de SparkSession pour le streaming
    val spark = SparkSession.builder()
      .appName("CsvSplitterWithStreaming")
      .master("local[*]")
      .getOrCreate()

    import spark.implicits._

    // Lecture des fichiers CSV en continu
    val streamingInputDF = spark.readStream
      .option("header", "true")
      .option("inferSchema", "true")
      .csv(outputDir)

    // Traitement : affichage des données ou autre logique
    val processedDF = streamingInputDF
      .withColumnRenamed("column1", "renamed_column1") // Exemple de traitement

    val query = processedDF.writeStream
      .outputMode("append") // Mode "append" pour les nouveaux fichiers
      .format("console") // Affiche les données dans la console
      .trigger(Trigger.ProcessingTime("5 seconds")) // Intervalle de déclenchement
      .start()

    query.awaitTermination()
  }
}
