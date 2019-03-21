package com.fashiontrade.query

import akka.actor.ActorSystem
import akka.event.Logging
import akka.kafka.Subscriptions
import akka.kafka.scaladsl.Consumer
import akka.kafka.scaladsl.Consumer.DrainingControl
import akka.stream.Materializer
import akka.stream.scaladsl.{Flow, Keep, Sink}
import akka.{Done, NotUsed}
import org.apache.kafka.clients.consumer.ConsumerRecord
import play.api.libs.json.Json

class RatingPipeline(config: PipelineConfig, searchService: SearchService)(
  implicit val system: ActorSystem,
  materializer: Materializer
) {
  val logger = Logging(system.eventStream, "rating-pipeline")

  def init(): DrainingControl[Done] = {
    val subscription = Subscriptions.topics("ratings")

    Consumer
      .plainSource(config.consumerSettings, subscription)
      .via(logMessage)
      .via(updateDress)
      .via(logIndexResponse)
      .toMat(Sink.ignore)(Keep.both)
      .mapMaterializedValue(DrainingControl.apply)
      .run()
  }

  private def logMessage = Flow[ConsumerRecord[String, String]].map { record =>
    logger.info(s"incoming rating message ${record.key()}")
    record
  }

  private def logIndexResponse = Flow[IndexUpdateResult].map { result =>
    logger.info(s"dress updated, success=${result.isSuccess} id=${result.docId}")
    result
  }

  private[query] def updateDress: Flow[ConsumerRecord[String, String], IndexUpdateResult, NotUsed] =
    Flow[ConsumerRecord[String, String]].map { record =>
      val jsonRecord = Json.parse(record.value())
      val dressId = (jsonRecord \ "payload" \ "dress_id").as[String]
      val stars = (jsonRecord \ "payload" \ "stars").as[Int]

      val getResult = searchService.getDocument(dressId)

      if (getResult.exists) {
        val dress = Json.parse(getResult.document).as[Dress]
        val updatedDress = updateDressRating(dress, stars)

        val indexResult = searchService.update(Json.toJson(updatedDress).toString(), dressId)
        if (indexResult.isSuccess) {
          IndexUpdateResult(isSuccess = true, dressId)
        } else {
          logger.warning(s"indexing failure, dressId=$dressId")
          IndexUpdateResult(isSuccess = false, dressId)
        }
      } else {
        logger.warning(s"missing document in index, dressId=$dressId")
        IndexUpdateResult(isSuccess = false, dressId)
      }
    }

  private def updateDressRating(dress: Dress, stars: Int) =
    dress
      .copy(starsCount = dress.starsCount + 1, starsMean = calculateMean(dress.starsMean, dress.starsCount, stars))

  private def calculateMean(existingMean: Double, starsCount: Int, newRating: Int) =
    ((existingMean * starsCount) + newRating) / (starsCount + 1)

}

case class IndexUpdateResult(isSuccess: Boolean, docId: String)
