package com.example.function

import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.kafka.{
  FlinkKafkaConsumer,
  FlinkKafkaProducer
}
import org.apache.flink.api.common.serialization.SimpleStringSchema
import org.apache.flink.api.common.state.MapStateDescriptor
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction
import org.apache.flink.util.Collector

import java.util.Properties
class UpdateWordWhitelistFunc extends BroadcastProcessFunction[String, String, String] {
  private val acceptedWordsStateDesc = new MapStateDescriptor[String, Boolean](
    "UpdateWordWhitelistState",
    classOf[String],
    classOf[Boolean]
  )

  override def processElement(
      value: String,
      ctx: BroadcastProcessFunction[String, String, String]#ReadOnlyContext,
      out: Collector[String]
  ): Unit = {
    val acceptedWords = ctx.getBroadcastState(acceptedWordsStateDesc)

    // If no accepted words are defined yet, let all words through
    var hasAcceptedWords = false
    val iter = acceptedWords.immutableEntries().iterator()
    if (iter.hasNext) {
      hasAcceptedWords = true
    }

    if (!hasAcceptedWords) {
      // If no accepted words defined, pass through all input
      out.collect(value)
    } else {
      // Filter based on accepted words
      value.toLowerCase
        .split("\\W+")
        .filter(_.nonEmpty)
        .foreach { word =>
          if (acceptedWords.contains(word.toLowerCase)) {
            out.collect(word)
          }
        }
    }
  }

  override def processBroadcastElement(
      value: String,
      ctx: BroadcastProcessFunction[String, String, String]#Context,
      out: Collector[String]
  ): Unit = {
    val acceptedWords = ctx.getBroadcastState(acceptedWordsStateDesc)
    // Add new accepted word to the state
    acceptedWords.put(value.toLowerCase.trim, true)
  }
}
