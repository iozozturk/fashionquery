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

class DressPipelineTest extends WordSpecLike with Matchers with MockitoSugar {
  implicit val actorSystem: ActorSystem = ActorSystem("dress-pipeline")
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  "DressPipeline" should {
    val searchService = mock[SearchService]
    val pipelineConfig = mock[PipelineConfig]
    val pipelineInTest = new DressPipeline(pipelineConfig, searchService)

    "index dress payload" in {
      val id = "id"
      val payload =
        s"""
           |{
           | "payload_key":"$id",
           | "payload":{}
           |}
        """.stripMargin

      val indexResult = IndexResult(isSuccess = true, id)
      when(searchService.upsert("{}", id)) thenReturn indexResult

      val flowUnderTest = pipelineInTest.indexOrUpdate

      val (pub, sub) = TestSource
        .probe[ConsumerRecord[String, String]]
        .via(flowUnderTest)
        .toMat(TestSink.probe[IndexResult])(Keep.both)
        .run()

      sub.request(1)
      pub.sendNext(new ConsumerRecord("dresses", 1, 1, id, payload))
      sub.expectNext(indexResult)
    }

  }

}
