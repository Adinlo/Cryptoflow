import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.types._
import org.apache.spark.sql.functions._

object CsvStreamingReader {

  def main(args: Array[String]): Unit = {
    val spark = SparkSession.builder()
      .appName("CSV Streaming Reader")
      .master("local[*]")
      .getOrCreate()

    val schema = StructType(Seq(
      StructField("Open time", StringType, true),
      StructField("Open", DoubleType, true),
      StructField("High", DoubleType, true),
      StructField("Low", DoubleType, true),
      StructField("Close", DoubleType, true),
      StructField("Volume", DoubleType, true),
    ))

    val inputDir = "streaming"

    /* val withTimestampDF = streamingDF
      .withColumn("timestamp", to_timestamp(col("Open time"), "yyyy-MM-dd HH:mm:ss"))

    val windowedStatsDF = withTimestampDF
      .withWatermark("timestamp", "1 minute")
      .groupBy(window(col("timestamp"), "10 seconds"))
      .agg(
        avg("High").as("avg_high"),
        min("Low").as("min_low"),
        max("Close").as("max_close"),
        avg("Volume").as("avg_volume")
      ) 
*/
    val streamingDF = spark.readStream
      .schema(schema)
      .option("header", "true")
      .csv(inputDir)

    val query = streamingDF.writeStream
      .format("console")
      .option("truncate", "false")
      .outputMode("append")  
      .start()

    query.awaitTermination()
  }
}