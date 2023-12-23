package com.radioretail.template.handles

import com.radioretail.common.rabbitmq.events
import com.fullfacing.skyhook.core.InternalServerError
//import com.fullfacing.skyhook.core.health.HealthCheck
import com.fullfacing.skyhook.core.protocol.ErrorResponse
import com.fullfacing.skyhook.rabbit.config.QueueConfig
import com.fullfacing.skyhook.rabbit.network.{Broker, RabbitHealthCheck}
import com.fullfacing.skyhook.rabbit.publisher.{Publisher, RpcPublisher}
import com.fullfacing.skyhook.rabbit.stream.Queue
import com.radioretail.template.api.RabbitApi
import com.radioretail.template.handles.Schedulers.fixed
import io.opentracing.Tracer
import monix.bio.IO
import monix.execution.Scheduler

final class Rabbit()(implicit scheduler: Scheduler, tracer: Tracer) extends RabbitHealthCheck {
  private val RABBITMQ_HOST         = sys.env("RABBITMQ_HOST")
  private val RABBITMQ_PORT         = sys.env("RABBITMQ_PORT").toInt
  private val RABBITMQ_USERNAME     = sys.env("RABBITMQ_USERNAME")
  private val RABBITMQ_PASSWORD     = sys.env("RABBITMQ_PASSWORD")
  private val RABBITMQ_VIRTUAL_HOST = sys.env("RABBITMQ_VIRTUAL_HOST")

  // Rabbit Connection
  private val brokerConfig = Broker.Config(
    host           = RABBITMQ_HOST,
    port           = RABBITMQ_PORT,
    username       = RABBITMQ_USERNAME,
    password       = RABBITMQ_PASSWORD,
    virtualHost    = RABBITMQ_VIRTUAL_HOST,
    sharedExecutor = Some(Schedulers.io)
  )

  private val qConfig = QueueConfig(
    name        = "rdr-svc-template",
    routingKeys = List.empty[String]
  )

  protected val broker = IO.from(Broker.apply(brokerConfig, tracer)).memoizeOnSuccess

  val publisher: IO[ErrorResponse, Publisher] = broker
    .flatMap(b => IO.from(b.createPublisher()))
    .mapError(InternalServerError("An error occurred while trying to create the Rabbit Publisher", _))

  val rpcPublisher: IO[ErrorResponse, RpcPublisher] = broker
    .flatMap(b => IO.from(b.createRpcPublisher()))
    .mapError(InternalServerError("An error occurred while trying to create the Rabbit RPC Publisher", _))

  val queue: IO[ErrorResponse, Queue] = broker
    .flatMap(b => IO.from(b.createQueue(qConfig)))
    .mapError(InternalServerError("An error occurred while trying to create the Rabbit Queue", _))
}