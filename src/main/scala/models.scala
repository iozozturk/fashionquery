import play.api.libs.json.JsPath
import play.api.libs.json.Json
import play.api.libs.json.Reads
import play.api.libs.json.Writes
import play.api.libs.functional.syntax.toFunctionalBuilderOps

case class Dress(
  id: String,
  images: Seq[DressImage],
  activationDate: String,
  name: String,
  color: String,
  season: String,
  price: Double,
  brand: Brand,
  starsCount: Int,
  starsMean: Double
)

object Dress {
  implicit val dressWrites = new Writes[Dress] {
    def writes(dress: Dress) = Json.obj(
      "id" -> dress.id,
      "images" -> dress.images,
      "activation_date" -> dress.activationDate,
      "name" -> dress.name,
      "color" -> dress.color,
      "season" -> dress.season,
      "price" -> dress.price,
      "brand" -> dress.brand,
      "stars_count" -> dress.starsCount,
      "stars_mean" -> dress.starsMean,
    )
  }

  implicit val yakReads: Reads[Dress] = (
    (JsPath \ "id").read[String] and
      (JsPath \ "images").read[Seq[DressImage]] and
      (JsPath \ "activation_date").read[String] and
      (JsPath \ "name").read[String] and
      (JsPath \ "color").read[String] and
      (JsPath \ "season").read[String] and
      (JsPath \ "price").read[Double] and
      (JsPath \ "brand").read[Brand] and
      (JsPath \ "stars_count").readWithDefault[Int](0) and
      (JsPath \ "stars_mean").readWithDefault[Double](0.0)
  )(Dress.apply(_, _, _, _, _, _, _, _, _, _))
}

case class Brand(logoUrl: String, name: String)

object Brand {

  implicit val brandWrites: Writes[Brand] = (brand: Brand) =>
    Json.obj(
      "logo_url" -> brand.logoUrl,
      "name" -> brand.name
  )

  implicit val brandReads: Reads[Brand] = (
    (JsPath \ "logo_url").readWithDefault[String]("") and
      (JsPath \ "name").read[String]
  )(Brand.apply(_, _))
}

case class DressImage(largeUrl: String, thumbUrl: String)

object DressImage {
  implicit val imageWrites: Writes[DressImage] = (image: DressImage) =>
    Json.obj(
      "large_url" -> image.largeUrl,
      "thumb_url" -> image.thumbUrl
  )

  implicit val imageReads: Reads[DressImage] = (
    (JsPath \ "large_url").read[String] and
      (JsPath \ "thumb_url").read[String]
  )(DressImage.apply(_, _))
}
