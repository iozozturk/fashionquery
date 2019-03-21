package com.fashiontrade.query

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.mockito.Mockito.when
import org.scalatest.{Matchers, WordSpecLike}
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json

class RatingPipelineTest extends WordSpecLike with Matchers with MockitoSugar {
  implicit val actorSystem: ActorSystem = ActorSystem("rating-pipeline")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  "RatingPipeline" should {
    val searchService = mock[SearchService]
    val pipelineConfig = mock[PipelineConfig]
    val pipelineInTest = new RatingPipeline(pipelineConfig, searchService)

    "index dress payload with updated rating data" in {
      val dressId = "dressId"
      val payload =
        s"""
           |{
           | "payload_key":"ratingId",
           | "payload":{
           |    "dress_id": "$dressId",
           |    "stars": 1
           | }
           |}
        """.stripMargin

      val getResult = GetResult(exists = true, fixture.dressJson, dressId)
      val indexResult = IndexResult(isSuccess = true, dressId)

      when(searchService.update(Json.parse(fixture.updatedDressWithStars).toString(), dressId)) thenReturn indexResult
      when(searchService.getDocument(dressId)) thenReturn getResult

      val flowUnderTest = pipelineInTest.updateDress

      val (pub, sub) = TestSource
        .probe[ConsumerRecord[String, String]]
        .via(flowUnderTest)
        .toMat(TestSink.probe[IndexUpdateResult])(Keep.both)
        .run()

      sub.request(1)
      pub.sendNext(new ConsumerRecord("dresses", 1, 1, "ratingId", payload))
      sub.expectNext(IndexUpdateResult(isSuccess = true, dressId))
    }

    "reply with unsuccessful when document does not exist" in {
      val dressId = "dressId"
      val payload =
        s"""
           |{
           | "payload_key":"ratingId",
           | "payload":{
           |    "dress_id": "$dressId",
           |    "stars": 1
           | }
           |}
        """.stripMargin
      val getResult = GetResult(exists = false, "{}", dressId)
      when(searchService.getDocument(dressId)) thenReturn getResult

      val flowUnderTest = pipelineInTest.updateDress

      val (pub, sub) = TestSource
        .probe[ConsumerRecord[String, String]]
        .via(flowUnderTest)
        .toMat(TestSink.probe[IndexUpdateResult])(Keep.both)
        .run()

      sub.request(1)
      pub.sendNext(new ConsumerRecord("dresses", 1, 1, "ratingId", payload))
      sub.expectNext(IndexUpdateResult(isSuccess = false, dressId))
    }

    "reply with unsuccessful when document could not be indexed" in {
      val dressId = "dressId"
      val payload =
        s"""
           |{
           | "payload_key":"ratingId",
           | "payload":{
           |    "dress_id": "$dressId",
           |    "stars": 1
           | }
           |}
        """.stripMargin
      val getResult = GetResult(exists = true, fixture.dressJson, dressId)
      when(searchService.getDocument(dressId)) thenReturn getResult

      val indexResult = IndexResult(isSuccess = false, dressId)
      when(searchService.update(Json.parse(fixture.updatedDressWithStars).toString(), dressId)) thenReturn indexResult

      val flowUnderTest = pipelineInTest.updateDress

      val (pub, sub) = TestSource
        .probe[ConsumerRecord[String, String]]
        .via(flowUnderTest)
        .toMat(TestSink.probe[IndexUpdateResult])(Keep.both)
        .run()

      sub.request(1)
      pub.sendNext(new ConsumerRecord("dresses", 1, 1, "ratingId", payload))
      sub.expectNext(IndexUpdateResult(isSuccess = false, dressId))
    }

  }

  object fixture {
    val dressJson = """
                      |{
                      |  "id": "dressId",
                      |  "images": [
                      |    {
                      |      "large_url": "http://i6.ztat.net/large_hd/AX/82/1C/A1/MQ/11/AX821CA1M-Q11@10.jpg",
                      |      "thumb_url": "http://i6.ztat.net/catalog_hd/AX/82/1C/A1/MQ/11/AX821CA1M-Q11@10.jpg"
                      |    },
                      |    {
                      |      "large_url": "http://i3.ztat.net/large_hd/AX/82/1C/A1/MQ/11/AX821CA1M-Q11@9.jpg",
                      |      "thumb_url": "http://i3.ztat.net/catalog_hd/AX/82/1C/A1/MQ/11/AX821CA1M-Q11@9.jpg"
                      |    }
                      |  ],
                      |  "activation_date": "2016-11-22T15:18:41+01:00",
                      |  "name": "Jersey dress - black",
                      |  "color": "Black",
                      |  "season": "WINTER",
                      |  "price": 24.04,
                      |  "brand": {
                      |    "logo_url": "https://i3.ztat.net/brand/9b3cabce-c405-44d7-a62f-ee00d5245962.jpg",
                      |    "name": "Anna Field Curvy"
                      |  },
                      |  "stars_count": 0,
                      |  "stars_mean": 0
                      |}
                    """.stripMargin

    val updatedDressWithStars =
      """
        |{
        |  "id": "dressId",
        |  "images": [
        |    {
        |      "large_url": "http://i6.ztat.net/large_hd/AX/82/1C/A1/MQ/11/AX821CA1M-Q11@10.jpg",
        |      "thumb_url": "http://i6.ztat.net/catalog_hd/AX/82/1C/A1/MQ/11/AX821CA1M-Q11@10.jpg"
        |    },
        |    {
        |      "large_url": "http://i3.ztat.net/large_hd/AX/82/1C/A1/MQ/11/AX821CA1M-Q11@9.jpg",
        |      "thumb_url": "http://i3.ztat.net/catalog_hd/AX/82/1C/A1/MQ/11/AX821CA1M-Q11@9.jpg"
        |    }
        |  ],
        |  "activation_date": "2016-11-22T15:18:41+01:00",
        |  "name": "Jersey dress - black",
        |  "color": "Black",
        |  "season": "WINTER",
        |  "price": 24.04,
        |  "brand": {
        |    "logo_url": "https://i3.ztat.net/brand/9b3cabce-c405-44d7-a62f-ee00d5245962.jpg",
        |    "name": "Anna Field Curvy"
        |  },
        |  "stars_count": 1,
        |  "stars_mean": 1
        |}
                                """.stripMargin
  }

}
