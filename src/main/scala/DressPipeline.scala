import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.Logging
import akka.kafka.Subscriptions
import akka.kafka.scaladsl.Consumer
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink}
import org.apache.kafka.clients.consumer.ConsumerRecord
import play.api.libs.json.{JsObject, Json}

class DressPipeline(config: PipelineConfig, indexService: IndexService)(
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
    logger.info(s"incoming dress message ${record.key()}")
    record
  }

  private def logIndexResponse = Flow[IndexResult].map { result =>
    logger.info(s"indexed document, success=${result.isSuccess}s id=${result.docId}")
    result
  }

  def indexOrUpdate: Flow[ConsumerRecord[String, String], IndexResult, NotUsed] =
    Flow[ConsumerRecord[String, String]].map { record =>
      val jsonRecord = Json.parse(record.value())
      val id = (jsonRecord \ "payload_key").as[String]
      val payload = (jsonRecord \ "payload").as[JsObject]

      indexService.upsert(payload.toString(), id)
    }
}
