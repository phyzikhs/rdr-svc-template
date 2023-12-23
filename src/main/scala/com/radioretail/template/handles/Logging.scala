package com.radioretail.template.handles

import io.sentry.event.EventBuilder
import io.sentry.event.helper.EventBuilderHelper
import io.sentry.{Sentry, SentryClient}
import org.slf4j.{Logger, LoggerFactory}

object Logging {
  System.setProperty("cats.effect.logNonDaemonThreadsOnExit", "false")

  private val ENVIRONMENT = sys.env("ENVIRONMENT")

  implicit val logger: Logger = LoggerFactory.getLogger("skyhook.application")

  private val decorator: EventBuilderHelper = (eventBuilder: EventBuilder) => {
    eventBuilder
      .withServerName("rdr-svc-template")
      .withEnvironment(ENVIRONMENT)
  }

  private val client: SentryClient = Sentry.init
  client.addBuilderHelper(decorator)

}