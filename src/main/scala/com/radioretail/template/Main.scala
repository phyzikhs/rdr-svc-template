package com.radioretail.template

import cats.effect.ExitCode
import com.fullfacing.skyhook.core.protocol.ErrorResponse
import com.fullfacing.skyhook.logging.LoggingImplicits._
import com.fullfacing.skyhook.mongo.formats.DefaultBsonFormats
import com.fullfacing.skyhook.rabbit.publisher.{Publisher, RpcPublisher}
import com.radioretail.template.handles.{Akka, Logging, Mongo, Rabbit, Schedulers, Shutdown, Tracing}
import Logging.logger
import com.fullfacing.skyhook.core.InternalServerError
import com.radioretail.template.api.RabbitApi
import com.radioretail.template.handles.Schedulers.fixed
import com.radioretail.template.services.templates.application.TemplateModule
import com.radioretail.template.services.templates.routes.TemplateRoutes

import io.opentracing.Tracer
import monix.bio.{BIOApp, IO, UIO}

object Main extends BIOApp {

  override def run(args: List[String]): UIO[ExitCode] = {
    new Main()
      .start()
      .map(_ => ExitCode.Success)
      .onErrorHandle { t =>
        logger.errorOpt(s"Failed to initiate Main module: ${t.buildMessage}", t.throwable)
        ExitCode.Error
      }
  }
}

class Main {
  def start(): IO[ErrorResponse, Unit] = {
    for {
      /** implicitly initialises tracer */
      implicit0(tracer: Tracer) <- Tracing.initialise()

      /** Mongo */
      mongo <- Mongo.connect(Schedulers.io, DefaultBsonFormats)

      /** Rabbit */
      rabbit = new Rabbit()
      implicit0(publisher: Publisher) <- rabbit.publisher
      implicit0(rpcPublisher: RpcPublisher) <- rabbit.rpcPublisher
      queue <- rabbit.queue

      // Service module
      templateModule = new TemplateModule(mongo.templateRepository)(publisher = publisher, bsonFormats = DefaultBsonFormats, scheduler = Schedulers.fixed, logger)

      // Service route
      templateRoutes = new TemplateRoutes()(module = templateModule, tracer = tracer)

      rabbitApi = new RabbitApi(templateModule = templateModule)(queue = queue, rpcPublisher = rpcPublisher, scheduler = Schedulers.io)

      // Handles
      _ <- Shutdown.handle(mongo)
      _ <- Akka.connect(templateRoutes.routes)
      _ <- rabbitApi.connect().mapError(InternalServerError("Could not connect to RabbitAPI", _))
    } yield ()
  }
}