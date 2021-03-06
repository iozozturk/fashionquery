package com.fashiontrade.query
import org.elasticsearch.action.support.WriteRequest.RefreshPolicy
import org.elasticsearch.common.xcontent.XContentType
import org.mockito.Mockito.when
import org.scalatest.Matchers
import org.scalatest.WordSpec
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.JsObject
import play.api.libs.json.Json

class SearchServiceTest extends WordSpec with Matchers with MockitoSugar {

  "SearchService" should {
    val indexConfig = mock[IndexConfig]
    val indexName = "test-index"
    when(indexConfig.indexName) thenReturn indexName
    val serviceInTest = new SearchService(indexConfig)
    val esClient = serviceInTest.esClient

    "get single existing document" in {
      val docId = "docId"
      val document =
        """
          |{ "test":"doc" }
        """.stripMargin

      esClient
        .prepareIndex(indexName, "_doc", docId)
        .setSource(document, XContentType.JSON)
        .get()

      serviceInTest.getDocument(docId) shouldEqual GetResult(exists = true, document, docId)
    }

    "reply with not exists when non existing document" in {
      val docId = "does-not-exist"

      serviceInTest.getDocument(docId) shouldEqual GetResult(exists = false, null, docId)
    }

    "upsert" in {
      val docId = "docId"
      val document =
        """
          |{ "newkey":"new value" }
        """.stripMargin

      esClient
        .prepareIndex(indexName, "_doc", docId)
        .setSource(document, XContentType.JSON)
        .get()

      serviceInTest.upsert(document, docId) shouldEqual IndexResult(isSuccess = true, docId)
    }

    "update" in {
      val docId = "docId"
      val document =
        """
          |{ "test":"doc" }
        """.stripMargin

      esClient
        .prepareIndex(indexName, "_doc", docId)
        .setSource(document, XContentType.JSON)
        .get()

      serviceInTest.update(document, docId) shouldEqual IndexResult(isSuccess = true, docId)
    }

    "search dress in dress name" in {
      esClient
        .prepareIndex(indexName, "_doc", fixture.dressId)
        .setSource(fixture.dressJson, XContentType.JSON)
        .setRefreshPolicy(RefreshPolicy.IMMEDIATE)
        .get()

      Json.parse(serviceInTest.searchDress(Some("dressName"), None).head).as[JsObject].toString() shouldEqual (Json
        .parse(fixture.dressJson)
        .as[JsObject] ++ Json.obj("score" -> 0))
        .toString()
    }

    "search dress in brand name" in {
      esClient
        .prepareIndex(indexName, "_doc", fixture.dressId)
        .setSource(fixture.dressJson, XContentType.JSON)
        .setRefreshPolicy(RefreshPolicy.IMMEDIATE)
        .get()

      Json.parse(serviceInTest.searchDress(Some("brandName"), None).head).as[JsObject].toString() shouldEqual (Json
        .parse(fixture.dressJson)
        .as[JsObject] ++ Json.obj("score" -> 0))
        .toString()
    }

    "search non existing dress" in {
      serviceInTest.searchDress(Some("some-non-existing-feature"), None) shouldEqual Seq()
    }

    "filter dress with brand name" in {
      esClient
        .prepareIndex(indexName, "_doc", fixture.dressId)
        .setSource(fixture.dressJson, XContentType.JSON)
        .setRefreshPolicy(RefreshPolicy.IMMEDIATE)
        .get()

      Json.parse(serviceInTest.searchDress(None, Some("brandName")).head).as[JsObject].toString() shouldEqual (Json
        .parse(fixture.dressJson)
        .as[JsObject] ++ Json.obj("score" -> 0))
        .toString()
    }

    object fixture {
      val dressId = "AX821CA1M-Q11"
      val dressJson = s"""
                         |{
                         |  "id": "${dressId}",
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
                         |  "name": "dressName",
                         |  "color": "Black",
                         |  "season": "WINTER",
                         |  "price": 24.04,
                         |  "brand": {
                         |    "logo_url": "https://i3.ztat.net/brand/9b3cabce-c405-44d7-a62f-ee00d5245962.jpg",
                         |    "name": "brandName"
                         |  },
                         |  "stars_count": 0,
                         |  "stars_mean": 0
                         |}
                      """.stripMargin
    }

  }
}
