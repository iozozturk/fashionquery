package com.fashiontrade.query

import java.net.InetAddress

import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.lucene.search.function.CombineFunction
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.TransportAddress
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.query.functionscore.{FunctionScoreQueryBuilder, ScoreFunctionBuilders}
import org.elasticsearch.transport.client.PreBuiltTransportClient
import play.api.libs.json.{JsObject, Json}

case class IndexResult(isSuccess: Boolean, docId: String)
case class GetResult(exists: Boolean, document: String, docId: String)

class SearchService(indexConfig: IndexConfig) {
  val esClient: TransportClient = new PreBuiltTransportClient(Settings.EMPTY)
    .addTransportAddress(new TransportAddress(InetAddress.getByName(indexConfig.hostAdress), 9300))
  private val indexName = indexConfig.indexName

  def upsert(document: String, id: String): IndexResult = {
    val response = esClient
      .prepareUpdate(indexName, "_doc", id)
      .setDoc(document, XContentType.JSON)
      .setDocAsUpsert(true)
      .get(SearchService.timeout)

    IndexResult(response.status().getStatus == 200, id)
  }

  def update(document: String, id: String): IndexResult = {
    val response = esClient
      .prepareUpdate(indexName, "_doc", id)
      .setDoc(document, XContentType.JSON)
      .get(SearchService.timeout)

    IndexResult(response.status().getStatus == 200, id)
  }

  def getDocument(id: String): GetResult = {
    val response = esClient
      .prepareGet(indexName, "_doc", id)
      .get(SearchService.timeout)

    GetResult(response.isExists, response.getSourceAsString, id)
  }

  def searchDress(query: String, brand: Option[String]): Seq[String] = {
    val queryBuilder =
      QueryBuilders.boolQuery().should(QueryBuilders.multiMatchQuery(query, "name", "brand.name", "season", "color"))
    if (brand.isDefined) {
      queryBuilder.must(QueryBuilders.termQuery("brand.name.keyword", brand.get))
    }

    val scoreFunction = ScoreFunctionBuilders
      .fieldValueFactorFunction("stars_mean")

    val response = esClient
      .prepareSearch(indexName)
      .setQuery(new FunctionScoreQueryBuilder(queryBuilder, scoreFunction).boostMode(CombineFunction.MULTIPLY))
      .get(SearchService.timeout)

    response.getHits.getHits.map { hit =>
      (Json.parse(hit.getSourceAsString).as[JsObject] ++ Json.obj("score" -> hit.getScore)).toString()
    }.toSeq
  }
}

object SearchService {
  private val timeout = TimeValue.timeValueSeconds(60)

}
