import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.Logging
import akka.kafka.Subscriptions
import akka.kafka.scaladsl.Consumer
import akka.stream.Materializer
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Sink
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.elasticsearch.action.update.UpdateResponse
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.xcontent.XContentType
import play.api.libs.json.JsObject
import play.api.libs.json.Json

object DressPipeline{
  val indexName = "fashion-dress"
}

class DressPipeline(config: PipelineConfig, esClient: TransportClient)(
  implicit val system: ActorSystem,
  materializer: Materializer
) {
  val logger = Logging(system.eventStream, "dress-pipeline")

  def init(): Consumer.Control = {
    val subscription = Subscriptions.topics("dresses")

    Consumer
      .plainSource(config.consumerSettings, subscription)
      .via(logMessage)
      .via(indexOrUpdate)
      .via(logIndexResponse)
      .to(Sink.ignore)
      .run()
  }

  private def logMessage = Flow[ConsumerRecord[String, String]].map { record =>
    logger.info(record.value())
    record
  }

  private def logIndexResponse = Flow[UpdateResponse].map { response =>
    logger.info(response.toString)
    response
  }

  def indexOrUpdate: Flow[ConsumerRecord[String, String], UpdateResponse, NotUsed] =
    Flow[ConsumerRecord[String, String]].map { record =>
      val jsonRecord = Json.parse(record.value())
      val id = (jsonRecord \ "payload_key").as[String]
      val payload = (jsonRecord \ "payload").as[JsObject]

      esClient
        .prepareUpdate(DressPipeline.indexName, "_doc", id)
        .setDoc(payload.toString(), XContentType.JSON)
        .setDocAsUpsert(true)
        .get()
    }
}

