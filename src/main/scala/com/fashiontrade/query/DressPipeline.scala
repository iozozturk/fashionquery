package com.fashiontrade.query

import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.event.Logging
import akka.kafka.Subscriptions
import akka.kafka.scaladsl.Consumer
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Keep, Sink}
import org.apache.kafka.clients.consumer.ConsumerRecord
import play.api.libs.json.Json

class DressPipeline(config: PipelineConfig, searchService: SearchService)(
  implicit val system: ActorSystem,
  materializer: Materializer
) {
  val logger = Logging(system.eventStream, "dress-pipeline")

  def init(): DrainingControl[Done] = {
    val subscription = Subscriptions.topics("dresses")

    Consumer
      .plainSource(config.consumerSettings, subscription)
      .via(logMessage)
      .via(indexOrUpdate)
      .via(logIndexResponse)
      .toMat(Sink.ignore)(Keep.both)
      .mapMaterializedValue(DrainingControl.apply)
      .run()
  }

  private def logMessage = Flow[ConsumerRecord[String, String]].map { record =>
    logger.info(s"incoming dress message ${record.key()}")
    record
  }

  private def logIndexResponse = Flow[IndexResult].map { result =>
    logger.info(s"indexed document, success=${result.isSuccess}s id=${result.docId}")
    result
  }

  private[query] def indexOrUpdate: Flow[ConsumerRecord[String, String], IndexResult, NotUsed] =
    Flow[ConsumerRecord[String, String]].map { record =>
      val jsonRecord = Json.parse(record.value())
      val id = (jsonRecord \ "payload_key").as[String]
      val payload = (jsonRecord \ "payload").as[Dress]

      searchService.upsert(Json.toJson(payload).toString(), id)
    }

}
