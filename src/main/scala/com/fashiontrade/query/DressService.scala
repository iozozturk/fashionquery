package com.fashiontrade.query

import org.elasticsearch.common.unit.TimeValue
import play.api.libs.json.Json

class DressService(indexService: SearchService) {
  private val timeout = TimeValue.timeValueSeconds(60)

  def search(query: String, brand: Option[String]): Seq[Dress] = {

    val response = indexService.searchDress(query, brand)

    response.map { doc =>
      Json.parse(doc).as[Dress]
    }
  }
}
