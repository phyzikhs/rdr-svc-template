package com.radioretail.template.handles

import cats.implicits.toFlatMapOps
import com.fullfacing.skyhook.core.InternalServerError
import com.fullfacing.skyhook.core.protocol.ErrorResponse
import io.jaegertracing.Configuration
import io.opentracing.Tracer
import io.opentracing.noop.NoopTracerFactory
import io.opentracing.util.GlobalTracer
import monix.bio.{IO, Task}
import org.mdedetrich.monix.opentracing.LocalScopeManager

class Tracing {

  protected val tracer: Task[Tracer] = {
    sys.env("ENVIRONMENT").split("-").lastOption match {
      case Some("prod") =>
        IO {
          Configuration
            .fromEnv()
            .getTracerBuilder
            .withScopeManager(new LocalScopeManager)
            .build()
        }

      case _ =>
        IO(NoopTracerFactory.create())
    }
  }
}

object Tracing {
  def initialise(): IO[ErrorResponse, Tracer] =
    new Tracing()
      .tracer
      .flatTap(t => IO(GlobalTracer.registerIfAbsent(t)))
      .mapError(InternalServerError("Failed to initialise Jaeger tracer", _))
}
