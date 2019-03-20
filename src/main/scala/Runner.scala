import java.net.InetAddress

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.ActorMaterializer
import akka.stream.ActorMaterializerSettings
import akka.stream.Supervision
import com.typesafe.config.ConfigFactory
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.TransportAddress
import org.elasticsearch.transport.client.PreBuiltTransportClient
import scala.concurrent.ExecutionContext.Implicits.global

object Runner extends App {
  implicit val system: ActorSystem = ActorSystem()
  val logger = Logging(system.eventStream, "fashion-query")

  val decider: Supervision.Decider = { e =>
    logger.error(s"Unhandled exception in stream: ${e.getMessage}", e)
    e.printStackTrace()
    Supervision.Stop
  }
  val materializerSettings = ActorMaterializerSettings(system).withSupervisionStrategy(decider)
  implicit val materializer: ActorMaterializer = ActorMaterializer(materializerSettings)

  val config = ConfigFactory.load
  val pipelineConfig = PipelineConfig(config, system)

  private val esHost = config.getString("elasticsearch.host")
  val esClient = new PreBuiltTransportClient(Settings.EMPTY)
    .addTransportAddress(new TransportAddress(InetAddress.getByName(esHost), 9300))

  new DressPipeline(pipelineConfig, esClient).init()
  new RatingPipeline(pipelineConfig, esClient).init()
  new Api(new QueryService(esClient)).init()
}
