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

    val flattenedDF = windowedDF
      .withColumn("window_start", col("window.start")) 
      .withColumn("window_end", col("window.end")) 
      .drop("window") 

    val jdbcUrl = "jdbc:postgresql://localhost:5432/Btc_db"
    val connectionProperties = new java.util.Properties()
    connectionProperties.put("user", "postgres")
    connectionProperties.put("password", "1234")
    connectionProperties.put("driver", "org.postgresql.Driver")

    val query = flattenedDF.writeStream
      .outputMode("append") 
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