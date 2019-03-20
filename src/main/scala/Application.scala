import java.net.InetAddress

import akka.Done
import akka.actor.{ActorSystem, CoordinatedShutdown}
import akka.event.Logging
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import com.typesafe.config.ConfigFactory
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.TransportAddress
import org.elasticsearch.transport.client.PreBuiltTransportClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationDouble

object Application extends App {
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

  private val indexService = new IndexService(esClient)
  private val dressService = new DressService(indexService)

  private val dressControl = new DressPipeline(pipelineConfig, indexService).init()
  private val ratingControl = new RatingPipeline(pipelineConfig, indexService).init()
  private val eventualBinding = new Api(dressService).init()

  CoordinatedShutdown(system).addTask(CoordinatedShutdown.PhaseServiceUnbind, "service_shutdown") { () =>
    logger.info("shutting down gracefully, terminating connections")
    eventualBinding.flatMap(_.terminate(hardDeadline = 30.second)).map { _ =>
      dressControl.shutdown()
      ratingControl.shutdown()
      esClient.close()
      Done
    }
  }
}
