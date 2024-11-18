package com.example

import com.example.function._
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.kafka.{
  FlinkKafkaConsumer,
  FlinkKafkaProducer
}
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.api.common.state.MapStateDescriptor
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction
import org.apache.flink.util.Collector
import org.apache.flink.api.common.restartstrategy.RestartStrategies

import java.util.concurrent.TimeUnit
import java.util.Properties
import org.apache.flink.api.common.time.Time

object WordCount {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    env.setRestartStrategy(
      RestartStrategies.fixedDelayRestart(
        3, // number of restart attempts
        Time.of(10, TimeUnit.SECONDS) // delay between attempts
      )
    )

    val properties = new Properties()
    properties.setProperty("bootstrap.servers", "kafka:9092")
    properties.setProperty("group.id", "word-count")

    val inputStream = env
      .addSource(
        new FlinkKafkaConsumer[String](
          "input",
          new SimpleStringSchema(),
          properties
        )
      )
      .uid("input-source")
      .name("Input Stream")

    val updateWordWhitelistStream = env
      .addSource(
        new FlinkKafkaConsumer[String](
          "word-whitelist",
          new SimpleStringSchema(),
          properties
        )
      )
      .uid("accepted-words-source")
      .name("Accepted Words Stream")

    // Define the descriptor as a val at object level for consistency
    val wordWhitelistStateDescriptor = new MapStateDescriptor[String, Boolean](
      "UpdateWordWhitelistState",
      classOf[String],
      classOf[Boolean]
    )

    val broadcastStream = updateWordWhitelistStream.broadcast(wordWhitelistStateDescriptor)
    val processedStream = inputStream
      .connect(broadcastStream)
      .process(new UpdateWordWhitelistFunc())
      .uid("word-filter")
      .name("Word Filter")

    val counts = processedStream
      .flatMap(_.toLowerCase.split("\\W+"))
      .filter(_.nonEmpty)
      .map((_, 1))
      .keyBy(_._1)
      .sum(1)

    counts
      .map(count => s"${count._1}:${count._2}")
      .addSink(
        new FlinkKafkaProducer[String](
          "output",
          new SimpleStringSchema(),
          properties
        )
      )
      .uid("output-sink")
      .name("Output Sink")

    env.execute("Word Count")
  }
}
