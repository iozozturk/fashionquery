import org.elasticsearch.client.transport.TransportClient
import org.elasticsearch.common.unit.TimeValue
import org.elasticsearch.common.xcontent.XContentType
import org.elasticsearch.index.query.QueryBuilders

case class IndexResult(isSuccess: Boolean, docId: String)
case class GetResult(exists: Boolean, document: String, docId: String)

class IndexService(esClient: TransportClient) {

  def upsert(document: String, id: String): IndexResult = {
    val response = esClient
      .prepareUpdate(IndexService.indexName, "_doc", id)
      .setDoc(document, XContentType.JSON)
      .setDocAsUpsert(true)
      .get(IndexService.timeout)

    IndexResult(response.status().getStatus == 200, id)
  }

  def index(document: String, id: String): IndexResult = {
    val response = esClient
      .prepareUpdate(IndexService.indexName, "_doc", id)
      .setDoc(document, XContentType.JSON)
      .get(IndexService.timeout)

    IndexResult(response.status().getStatus == 200, id)
  }

  def getDocument(id: String): GetResult = {
    val response = esClient
      .prepareGet(IndexService.indexName, "_doc", id)
      .get(IndexService.timeout)

    GetResult(response.isExists, response.getSourceAsString, id)
  }

  def searchDress(query: String): Seq[String] = {
    val response = esClient
      .prepareSearch(IndexService.indexName)
      .setQuery(QueryBuilders.multiMatchQuery(query, "name", "brand.name", "season", "color"))
      .get(IndexService.timeout)

    response.getHits.getHits.map { hit =>
      hit.getSourceAsString
    }.toSeq
  }
}

object IndexService {
  private val indexName = "fashion-dress"
  private val timeout = TimeValue.timeValueSeconds(60)
}
