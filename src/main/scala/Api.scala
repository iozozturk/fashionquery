import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.stream.Materializer
import de.heikoseeberger.akkahttpplayjson.PlayJsonSupport

import scala.concurrent.{ExecutionContext, Future}

class Api(queryService: QueryService)(
  implicit val system: ActorSystem,
  val materializer: Materializer,
  val executionContext: ExecutionContext
) extends PlayJsonSupport {
  val logger = Logging(system.eventStream, "dress-api")

  def init(): Future[Unit] = {
    val (host, port) = ("localhost", 8080)
    Http().bindAndHandle(route, host, port).map { binding =>
      logger.info(s"Dress api is listening on $host:$port")
    }
  }

  private val route = get {
    path("search") {
      parameters('query, 'brand.?) { (query, brand) =>
        logger.info(s"new search for query=$query brand=$brand")
        val queryHits = queryService.search(query, brand)
        complete(OK, queryHits)
      }
    }
  }
}
