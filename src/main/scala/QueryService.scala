import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.index.query.QueryBuilders
import play.api.libs.json.{JsObject, Json}

class QueryService(esClient: TransportClient) {
  private val timeout = TimeValue.timeValueSeconds(60)

  def search(query: String, brand: Option[String]): Seq[JsObject] = {
    val response = esClient
      .prepareSearch(DressPipeline.indexName)
      .setQuery(QueryBuilders.multiMatchQuery(query, "name", "brand.name", "season", "color"))
      .get(timeout)

    response.getHits.getHits.map { hit =>
      Json.parse(hit.getSourceAsString).as[JsObject]
    }.toSeq
  }
}
