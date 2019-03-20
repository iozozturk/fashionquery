import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes.OK
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.directives.MethodDirectives.get
import akka.stream.Materializer

import scala.concurrent.{ExecutionContext, Future}

class Api(implicit val system: ActorSystem, val materializer: Materializer, val executionContext: ExecutionContext) {
  val logger = Logging(system.eventStream, "dress-api")

  def init(): Future[Unit] = {
    val (host, port) = ("localhost", 8080)
    Http().bindAndHandle(route, host, port).map { binding =>
      logger.info(s"Dress api is listening on $host:$port")
    }
  }

  private val route = get {
    path("search") {
      complete(OK)
    }
  }
}
