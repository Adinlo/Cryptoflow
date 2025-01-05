// import scala.io.Source
// import java.io.{File, PrintWriter}

// // READSTREAM

// object CsvSplitter {

//   def main(args: Array[String]): Unit = {
//     val singleCsvPath = "src/resources/src/resources/btc_4h_data_2018_to_2024-2024-12-10(1).csv"
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
import scala.io.Source
import java.io.{File, PrintWriter}
import java.nio.file.{Files, Paths}
import scala.concurrent.{ExecutionContext, Future}

object CsvSplitter {

  def main(args: Array[String]): Unit = {
    val singleCsvPath = "src/resources/src/resources/btc_4h_data_2018_to_2024-2024-12-10(1).csv"
    val outputDir = "streaming"

    // Charger le fichier CSV complet
    val lines = Source.fromFile(singleCsvPath).getLines().toList
    val header = lines.head
    val dataLines = lines.tail

    val chunkSize = 10
    var chunkIndex = 0

    // Créer le répertoire de sortie s'il n'existe pas
    val outputDirectory = new File(outputDir)
    if (!outputDirectory.exists()) {
      outputDirectory.mkdirs()
    }

    // Découper les lignes du fichier en morceaux et les écrire
    while (chunkIndex * chunkSize < dataLines.length) {
      val chunk = dataLines.slice(chunkIndex * chunkSize, (chunkIndex + 1) * chunkSize)
      val chunkFileName = s"$outputDir/chunk$chunkIndex.csv"
      val writer = new PrintWriter(new File(chunkFileName))

      writer.println(header)
      chunk.foreach(writer.println)
      writer.close()

      println(s"Wrote $chunkFileName with ${chunk.size} lines.")
      chunkIndex += 1

      // Simuler une arrivée de fichier toutes les 5 secondes
      Thread.sleep(5000)
    }
  }
}
