package com.radioretail.template.handles

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import com.fullfacing.skyhook.core.InternalServerError
import com.fullfacing.skyhook.core.protocol.ErrorResponse

import com.radioretail.template.handles.Logging.logger
import monix.bio.{IO, Task}

object Akka {
  private implicit val act: ActorSystem = ActorSystem("akka-http", defaultExecutionContext = Some(Schedulers.io))

  def connect(routes: Route): IO[ErrorResponse, Unit] = IO.deferFutureAction { implicit ctx =>
    Http()
      .newServerAt("0.0.0.0", 8192)
      .bindFlow(routes)
      .map( binding => logger.info(s"Template API listening on ${binding.localAddress.getHostString}:${binding.localAddress.getPort}") )
  }.mapError{error =>
    logger.error("Error initialising the svc server!", error)
    InternalServerError("Failed to initialise Akka")
  }

  def shutdown(): Task[Unit] = IO.deferFutureAction { _ =>
    logger.info("Shutting down Akka-HTTP connection pools...")
    Http().shutdownAllConnectionPools()
  }

}