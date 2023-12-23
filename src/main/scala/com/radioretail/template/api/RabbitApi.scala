package com.radioretail.template.api

import com.fullfacing.skyhook.json.serializers.JsonFormats.defaults
import com.fullfacing.skyhook.rabbit.publisher.RpcPublisher
import com.fullfacing.skyhook.rabbit.stream.{Queue, ResponseConsumer}
import com.radioretail.template.TemplateTopics
import com.radioretail.template.domain.{Template, TemplateRequest}
import com.radioretail.template.services.templates.application.TemplateModule
import monix.bio.Task
import monix.execution.Scheduler

class RabbitApi(templateModule: TemplateModule)
               (implicit queue: Queue, rpcPublisher: RpcPublisher, scheduler: Scheduler) {

  def render(): Task[Unit] = {
    queue
      .subscribeToKey[TemplateRequest](TemplateTopics.Template.RenderSingle)
      .mapEval(request => templateModule.process(request).attempt)
      .consumeWith(new ResponseConsumer[Template]())
  }

  def renderList(): Task[Unit] = {
    queue
      .subscribeToKey[List[TemplateRequest]](TemplateTopics.Template.RenderList)
      .mapEval(request => templateModule.processL(request).attempt)
      .consumeWith(new ResponseConsumer[List[Template]]())
  }

  def connect(): Task[Unit] = {
    for {
      _ <- render().startAndForget
      _ <- renderList().startAndForget
      _ <- queue.connect()
      _ <- rpcPublisher.connect()
    } yield ()
  }
}