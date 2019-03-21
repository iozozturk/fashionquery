package com.fashiontrade.query
import org.elasticsearch.common.xcontent.XContentType
import org.mockito.Mockito.when
import org.scalatest.{Matchers, WordSpec}
import org.scalatestplus.mockito.MockitoSugar

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

      val response = esClient
        .prepareIndex(indexName, "_doc", docId)
        .setSource(document, XContentType.JSON)
        .get()

      print(response.toString)

      serviceInTest.getDocument(docId) shouldEqual GetResult(exists = true, document, docId)
    }

    "upsert" in {}

    "index" in {}

    "searchDress" in {}

  }
}
