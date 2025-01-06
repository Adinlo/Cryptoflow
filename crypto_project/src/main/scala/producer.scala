// import scala.io.Source
// import java.io.{File, PrintWriter}
// import java.nio.file.{Files, Paths}
// import scala.concurrent.{ExecutionContext, Future}

// object CsvSplitter {

//   def main(args: Array[String]): Unit = {
//     val singleCsvPath = "src/resources/src/resources/btc_4h_data_2018_to_2024-2024-12-10.csv"
//     val outputDir = "streaming"

//     val lines = Source.fromFile(singleCsvPath).getLines().toList
//     val header = lines.head
//     val dataLines = lines.tail

//     val chunkSize = 10
//     var chunkIndex = 0

//     val outputDirectory = new File(outputDir)
//     if (!outputDirectory.exists()) {
//       outputDirectory.mkdirs()
//     }

//     while (chunkIndex * chunkSize < dataLines.length) {
//       val chunk = dataLines.slice(chunkIndex * chunkSize, (chunkIndex + 1) * chunkSize)
//       val chunkFileName = s"$outputDir/chunk$chunkIndex.csv"
//       val writer = new PrintWriter(new File(chunkFileName))

//       writer.println(header)
//       chunk.foreach(writer.println)
//       writer.close()

//       println(s"Wrote $chunkFileName with ${chunk.size} lines.")
//       chunkIndex += 1

//       Thread.sleep(5000)
//     }
//   }
// }

import java.util.Properties
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}

object CsvKafkaProducer {
  def main(args: Array[String]): Unit = {
    val csvFilePath = "src/resources/src/resources/btc_4h_data_2018_to_2024-2024-12-10.csv" 
    val kafkaTopic = "btc_topic" 
    val bootstrapServers = "localhost:9092" 

    val props = new Properties()
    props.put("bootstrap.servers", bootstrapServers)
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

    val producer = new KafkaProducer[String, String](props)

    try {
      val source = scala.io.Source.fromFile(csvFilePath)
      val lines = source.getLines().toList
      val header = lines.head 
      val dataLines = lines.tail

      dataLines.foreach { line =>
        val record = new ProducerRecord[String, String](kafkaTopic, null, line)
        producer.send(record)
        println(s"Message envoyé à Kafka: $line") // pas de println
        Thread.sleep(1000) 
      }
      source.close()
    } finally {
      producer.close()
    }
  }
}


