package com.fashiontrade.query

import akka.actor.ActorSystem
import akka.kafka.ConsumerSettings
import com.typesafe.config.Config
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer

case class PipelineConfig(consumerSettings: ConsumerSettings[String, String])

object PipelineConfig {
  val deserializer = new StringDeserializer

  def apply(config: Config, system: ActorSystem): PipelineConfig =
    new PipelineConfig(buildConsumerSettings(config, system))

  private def kafkaHost(config: Config) = config.getString("kafka.host")

  private def buildConsumerSettings(config: Config, system: ActorSystem) =
    ConsumerSettings(system, deserializer, deserializer)
      .withBootstrapServers(kafkaHost(config))
      .withGroupId("pipeline")
      .withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")
}

case class IndexConfig(indexName: String, hostAdress: String)

object IndexConfig {

  def apply(config: Config, system: ActorSystem): IndexConfig =
    new IndexConfig(esIndexName(config), esHostAddress(config))

  private def esHostAddress(config: Config) = config.getString("elasticsearch.host")

  private def esIndexName(config: Config) = config.getString("elasticsearch.index")

}
