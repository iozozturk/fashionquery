import org.scalatest.Matchers
import org.scalatest.WordSpec
import play.api.libs.json.Json

class DressTest extends WordSpec with Matchers {

  val dress = Dress(
    id = "AX821CA1M-Q11",
    images = Seq(
      DressImage(
        "http://i6.ztat.net/large_hd/AX/82/1C/A1/MQ/11/AX821CA1M-Q11@10.jpg",
        "http://i6.ztat.net/catalog_hd/AX/82/1C/A1/MQ/11/AX821CA1M-Q11@10.jpg"
      ),
      DressImage(
        "http://i3.ztat.net/large_hd/AX/82/1C/A1/MQ/11/AX821CA1M-Q11@9.jpg",
        "http://i3.ztat.net/catalog_hd/AX/82/1C/A1/MQ/11/AX821CA1M-Q11@9.jpg"
      )
    ),
    activationDate = "2016-11-22T15:18:41+01:00",
    name = "Jersey dress - black",
    color = "Black",
    season = "WINTER",
    price = 24.04,
    brand = Brand("https://i3.ztat.net/brand/9b3cabce-c405-44d7-a62f-ee00d5245962.jpg", "Anna Field Curvy"),
    starsCount = 0,
    starsMean = 0.0
  )

  val dressJson = """
                    |{
                    |  "id": "AX821CA1M-Q11",
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

  "Dress model" should {

    "serialize into json" in {
      Json.toJson(dress) shouldEqual Json.parse(dressJson)
    }

    "deserialize from json" in {
      Json.parse(dressJson).as[Dress] shouldEqual dress
    }
  }

}
