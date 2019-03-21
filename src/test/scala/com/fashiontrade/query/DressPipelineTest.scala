package com.fashiontrade.query

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.TestSink
import akka.stream.testkit.scaladsl.TestSource
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.mockito.Mockito.when
import org.scalatest.Matchers
import org.scalatest.WordSpecLike
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.Json

class DressPipelineTest extends WordSpecLike with Matchers with MockitoSugar {
  implicit val actorSystem: ActorSystem = ActorSystem("dress-pipeline")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  "DressPipeline" should {
    val searchService = mock[SearchService]
    val pipelineConfig = mock[PipelineConfig]
    val pipelineInTest = new DressPipeline(pipelineConfig, searchService)

    "index dress payload" in {
      val payload =
        s"""
           |{
           | "payload_key":"${fixture.dressId}",
           | "payload": ${fixture.dressJson}
           |}
        """.stripMargin

      val indexResult = IndexResult(isSuccess = true, fixture.dressId)
      when(searchService.upsert(Json.parse(fixture.dressJson).toString(), fixture.dressId)) thenReturn indexResult

      val flowUnderTest = pipelineInTest.indexOrUpdate

      val (pub, sub) = TestSource
        .probe[ConsumerRecord[String, String]]
        .via(flowUnderTest)
        .toMat(TestSink.probe[IndexResult])(Keep.both)
        .run()

      sub.request(1)
      pub.sendNext(new ConsumerRecord("dresses", 1, 1, fixture.dressId, payload))
      sub.expectNext(indexResult)
    }

    "index dress without stars with stars fields" in {
      val payload =
        s"""
           |{
           | "payload_key":"${fixture.dressId}",
           | "payload": ${fixture.dressWithoutStarsJson}
           |}
        """.stripMargin

      val indexResult = IndexResult(isSuccess = true, fixture.dressId)
      when(searchService.upsert(Json.parse(fixture.dressJson).toString(), fixture.dressId)) thenReturn indexResult

      val flowUnderTest = pipelineInTest.indexOrUpdate

      val (pub, sub) = TestSource
        .probe[ConsumerRecord[String, String]]
        .via(flowUnderTest)
        .toMat(TestSink.probe[IndexResult])(Keep.both)
        .run()

      sub.request(1)
      pub.sendNext(new ConsumerRecord("dresses", 1, 1, fixture.dressId, payload))
      sub.expectNext(indexResult)
    }

    object fixture {
      val dressId = "AX821CA1M-Q11"
      val dressJson = s"""
                        |{
                        |  "id": "$dressId",
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
                        |  "stars_mean": 2.5
                        |}
                      """.stripMargin

      val dressWithoutStarsJson = s"""
                        |{
                        |  "id": "$dressId",
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
                        |  }
                        |}
                      """.stripMargin
    }

  }

}
