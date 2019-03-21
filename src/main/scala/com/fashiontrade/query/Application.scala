package com.fashiontrade.query
import akka.actor.{ActorSystem, CoordinatedShutdown}
import akka.event.Logging
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import com.typesafe.config.ConfigFactory

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
  val indexConfig = IndexConfig(config, system)

  private val searchService = new SearchService(indexConfig)

  private val dressControl = new DressPipeline(pipelineConfig, searchService).init()
  private val ratingControl = new RatingPipeline(pipelineConfig, searchService).init()
  private val eventualBinding = new Api(searchService).init()

  CoordinatedShutdown(system).addTask(CoordinatedShutdown.PhaseServiceUnbind, "service_shutdown") { () =>
    logger.info("shutting down gracefully, terminating connections")
    eventualBinding.flatMap(_.terminate(hardDeadline = 30.second)).flatMap { _ =>
      searchService.esClient.close()
      dressControl.drainAndShutdown()
      ratingControl.drainAndShutdown()
    }
  }
}
