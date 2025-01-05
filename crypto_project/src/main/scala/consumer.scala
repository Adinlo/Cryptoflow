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
      .option("inferSchema", "false") 
      .schema(schema) 
      .csv(inputDir)

    val processedDF = streamingInputDF
      .withColumnRenamed("Open", "open_price")  

    val query = processedDF.writeStream
      .outputMode("append")  
      .format("console")     /
      // .trigger(Trigger.ProcessingTime("5 seconds"))
      .start()

    query.awaitTermination()
  }
}
