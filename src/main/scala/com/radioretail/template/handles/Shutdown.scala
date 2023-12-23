package com.radioretail.template.handles

import Logging.logger
import Schedulers.fixed
import com.radioretail.common.utils.databases.masterelect.MasterElect
import monix.bio.UIO

import scala.concurrent.duration._

object Shutdown {

  /** Adds a shutdown hook to safely close all resources before the application is terminated. */
  def handle(mongo: Mongo): UIO[Unit] = UIO {
    Runtime.getRuntime.addShutdownHook {
      new Thread() {
        override def run(): Unit = {
          (for {
            _ <- UIO(logger.warn("Shutdown Received"))
            _ <- Akka.shutdown()
            _ <- mongo.shutdown()
          } yield Schedulers.shutdown())
            .onErrorHandle(t => logger.error("Error during shutdown", t))
            .runSyncUnsafe(20.seconds)
        }
      }
    }
  }
}