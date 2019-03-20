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
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.common.xcontent.XContentType
import play.api.libs.json.JsNumber
import play.api.libs.json.JsObject
import play.api.libs.json.Json

class RatingPipeline(config: PipelineConfig, esClient: TransportClient)(
  implicit val system: ActorSystem,
  materializer: Materializer
) {
  val logger = Logging(system.eventStream, "rating-pipeline")

  def init(): Consumer.Control = {
    val subscription = Subscriptions.topics("ratings")

    Consumer
      .plainSource(config.consumerSettings, subscription)
      .via(logMessage)
      .via(updateDress)
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

  def updateDress = Flow[ConsumerRecord[String, String]].map { record =>
    val timeout = TimeValue.timeValueSeconds(3)
    val jsonRecord = Json.parse(record.value())
    val dressId = (jsonRecord \ "payload" \ "dress_id").as[String]
    val stars = (jsonRecord \ "payload" \ "stars").as[Int]

    val response = esClient
      .prepareGet("fashion-dress", "_doc", dressId)
      .get(timeout)

    val starsCount = (Json.parse(response.getSourceAsString) \ "stars-count").getOrElse(JsNumber(0)).as[Int]
    val starsMean = (Json.parse(response.getSourceAsString) \ "stars-mean").getOrElse(JsNumber(0.0)).as[Double]

    val updatedDress = Json.parse(response.getSourceAsString).as[JsObject] ++ Json
      .obj("stars-count" -> (starsCount + 1), "stars-mean" -> calculateMean(starsMean, starsCount, stars))

    val updateResponse = esClient
      .prepareUpdate("fashion-dress", "_doc", dressId)
      .setDoc(updatedDress.toString(), XContentType.JSON)
      .get(timeout)

    updateResponse
  }

  private def calculateMean(existingMean: Double, starsCount: Int, newRating: Int) =
    ((existingMean * starsCount) + newRating) / (starsCount + 1)

}
