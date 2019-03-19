import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.Logging
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Sink}
import com.typesafe.config.Config
import org.apache.kafka.clients.consumer.{ConsumerConfig, ConsumerRecord}
import org.apache.kafka.common.serialization.StringDeserializer
import org.elasticsearch.action.update.UpdateResponse
import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.xcontent.XContentType
import play.api.libs.json.{JsObject, Json}

class Pipeline(config: Config, esClient: TransportClient)(implicit val system: ActorSystem, materializer: Materializer) {
  val logger = Logging(system.eventStream, "pipeline")

  def init(): Unit = {
    val deserializer = new StringDeserializer

    val kafkaHost = config.getString("kafka.host")

    val consumerSettings =
      ConsumerSettings(system, deserializer, deserializer)
        .withBootstrapServers(kafkaHost)
        .withGroupId("pipeline")
        .withProperty(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true")


    val subscription = Subscriptions.topics("dresses")
    Consumer.plainSource(consumerSettings, subscription)
      .via(logMessage)
      .via(indexOrUpdate)
      .runWith(Sink.foreach(response => print(response.toString)))

  }

  def logMessage = Flow[ConsumerRecord[String, String]].map { record =>
    logger.info(record.value())
    record
  }

  def indexOrUpdate: Flow[ConsumerRecord[String, String], UpdateResponse, NotUsed] = Flow[ConsumerRecord[String, String]].map { record =>
    val jsonRecord = Json.parse(record.value())
    val id = (jsonRecord \ "payload_key").as[String]
    val payload = (jsonRecord \ "payload").as[JsObject]

    esClient.prepareUpdate("fashionquery", "_doc", id)
      .setDoc(payload.toString(), XContentType.JSON)
      .setDocAsUpsert(true)
      .get()
  }


}
