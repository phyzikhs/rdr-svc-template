package com.radioretail.template.handles

import Logging.logger
import monix.execution.Scheduler
import monix.execution.schedulers.{SchedulerService, TracingScheduler}

object Schedulers {
  private val fixedService: SchedulerService = Scheduler.fixedPool("templates-fixed", 8)
  private val ioService: SchedulerService = Scheduler.io("templates-io")

  implicit val fixed: TracingScheduler = TracingScheduler(fixedService)
  val io: TracingScheduler = TracingScheduler(ioService)

  def shutdown(): Unit = {
    logger.info("Shutting down Monix schedulers...")
    fixedService.shutdown()
    ioService.shutdown()
  }
}