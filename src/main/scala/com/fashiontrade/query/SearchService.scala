package com.fashiontrade.query

import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.index.query.QueryBuilders

case class IndexResult(isSuccess: Boolean, docId: String)
case class GetResult(exists: Boolean, document: String, docId: String)

class SearchService(esClient: TransportClient) {

  def upsert(document: String, id: String): IndexResult = {
    val response = esClient
      .prepareUpdate(SearchService.indexName, "_doc", id)
      .setDoc(document, XContentType.JSON)
      .setDocAsUpsert(true)
      .get(SearchService.timeout)

    IndexResult(response.status().getStatus == 200, id)
  }

  def index(document: String, id: String): IndexResult = {
    val response = esClient
      .prepareUpdate(SearchService.indexName, "_doc", id)
      .setDoc(document, XContentType.JSON)
      .get(SearchService.timeout)

    IndexResult(response.status().getStatus == 200, id)
  }

  def getDocument(id: String): GetResult = {
    val response = esClient
      .prepareGet(SearchService.indexName, "_doc", id)
      .get(SearchService.timeout)

    GetResult(response.isExists, response.getSourceAsString, id)
  }

  def searchDress(query: String): Seq[String] = {
    val response = esClient
      .prepareSearch(SearchService.indexName)
      .setQuery(QueryBuilders.multiMatchQuery(query, "name", "brand.name", "season", "color"))
      .get(SearchService.timeout)

    response.getHits.getHits.map { hit =>
      hit.getSourceAsString
    }.toSeq
  }
}

object SearchService {
  private val indexName = "fashion-dress"
  private val timeout = TimeValue.timeValueSeconds(60)
}
