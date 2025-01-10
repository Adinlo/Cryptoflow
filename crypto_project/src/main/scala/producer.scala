import org.apache.spark.sql.SparkSession
import org.apache.kafka.clients.producer.{KafkaProducer, ProducerRecord}
import java.util.Properties

object CsvKafkaProducer {
  def main(args: Array[String]): Unit = {
    val csvPath = "src/resources/src/resources/btc_4h_data_2018_to_2024-2024-12-10.csv"
    val kafkaTopic = "btc_topic_1"
    val bootstrapServers = "localhost:9092"

    val spark = SparkSession.builder()
      .appName("CsvKafkaProducer")
      .master("local[*]")
      .getOrCreate()

    val df = spark.read
      .option("header", "true")
      .csv(csvPath)

    val props = new Properties()
    props.put("bootstrap.servers", bootstrapServers)
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

    val producer = new KafkaProducer[String, String](props)

    try {
      val dataLines = df.collect()

      dataLines.foreach { row =>
        val line = row.mkString(",")
        val record = new ProducerRecord[String, String](kafkaTopic, null, line)
        producer.send(record)
        Thread.sleep(1000) 
      }
    } finally {
      producer.close()
      spark.stop()
    }
  }
}