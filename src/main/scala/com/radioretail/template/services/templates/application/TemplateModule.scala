package com.radioretail.template.services.templates.application

import cats.implicits._
import com.fullfacing.skyhook.core.protocol.{DataResponse, EmptyResponse, ErrorResponse}
import com.fullfacing.skyhook.core.{Created, InternalServerError, NoContent, Ok}
import com.fullfacing.skyhook.mongo.formats.BsonFormats
import com.fullfacing.skyhook.rabbit.publisher.Publisher
import com.fullfacing.skyhook.rest.services.{Delete, Retrieve, Update}
import com.radioretail.template.domain.{Template, TemplateRequest}
import com.radioretail.template.services.templates.application.engine.{StringRenderResult, StringTemplateLoader, TableData}
import com.radioretail.template.services.templates.persistence.TemplateRepository
import de.zalando.beard.renderer.{BeardTemplateRenderer, CustomizableTemplateCompiler, TemplateName}
import monix.bio.IO
import monix.execution.Scheduler
import org.slf4j.Logger

import java.time.LocalDateTime
import java.util.UUID
import scala.collection.immutable.Seq
import scala.util.{Failure, Success}

final case class TemplateModule(collection: TemplateRepository)
                               (implicit publisher: Publisher,
                                bsonFormats: BsonFormats,
                                scheduler: Scheduler,
                                logger: Logger)
  extends Retrieve[Template, Template.Fetch, Template.Query]
    with Update[Template, Template.Update]
    with Delete[Template] {

  def mapper(create: Template.Create): Template =
    Template(
      name          = create.name,
      data          = create.data,
      `type`        = create.`type`,
      languageCode  = create.languageCode,
      configuration = create.configuration,
      createdAt     = LocalDateTime.now(),
      updatedAt     = LocalDateTime.now(),
      id            = UUID.randomUUID(),
      deleted       = false
    )

  def createMany(creates: Seq[Template.Create]): IO[ErrorResponse, EmptyResponse] = {
    collection
      .insertMany(creates.map(mapper))
      .mapError(_ => InternalServerError("An error occurred while creating the templates."))
      .map(_ => NoContent.apply)
  }

  def create(create: Template.Create): IO[ErrorResponse, DataResponse[Template]] = {
    val template = mapper(create)

    collection
      .insert(template)
      .mapError(_ => InternalServerError("An error occurred while creating the templates."))
      .map(_ => Created(template))
  }

  /**
   * Replaces a Template by name and type. If no Template with the given exists it will be inserted instead.
   *
   * @param body The Create submodel of the Template to be replaced/inserted
   * @return Unit
   */
  def replace(body: Template.Create): IO[ErrorResponse, DataResponse[Template]] = {
    val template = mapper(body)

    collection
      .replace(template)
      .map(_ => Ok(template))
  }

  /**
    * Loads our template from the database, the template has a composite primary key made up of the input values.
    *
    * @param req Request object.
    * @return A optional Template instance.
    */
  def load(req: TemplateRequest): IO[ErrorResponse, Template] = {
    collection.fetchOne(req)
  }

  /**
    * Renders a template using the Beard Templating Engine.
    *
    * @param name     The name of the template.
    * @param template The template in it's raw string form from the database.
    * @param data     The serialized Json data for the template engine.
    * @return The rendered template in string form.
    */
  private def render(name: String, template: String, data: Map[String, Any]): IO[ErrorResponse, String] = IO.fromEither {
    val loader   = StringTemplateLoader(template)
    val compiler = new CustomizableTemplateCompiler(templateLoader = loader)
    val renderer = new BeardTemplateRenderer(compiler, Seq(TableData()))

    compiler.compile(TemplateName(name)) match {
      case Success(t) =>
        renderer.render(t, StringRenderResult(), data).asRight
      case Failure(e) =>
        logger.debug(e.getMessage)
        InternalServerError("An error occurred while rendering the template.").asLeft
    }
  }

  /**
    * Renders a Template from a TemplateRequest Request.
    *
    * @param req The request object containing all the information for the Template Engine.
    * @return The rendered Template in String form.
    */
  def process(req: TemplateRequest): IO[ErrorResponse, DataResponse[Template]] = {
    (for {
      template <- load(req)
      message  <- render(template.name, template.data, req.data)
    } yield Ok(template
      .copy(data = message)))
      .mapError(_ => InternalServerError("An error occurred while rendering the template."))
  }

  /**
    * Renders a Template from a TemplateRequest Request.
    *
    * @param requests The request object containing all the information for the Template Engine.
    * @return The rendered Template in String form.
    */
  def processL(requests: List[TemplateRequest]): IO[ErrorResponse, DataResponse[List[Template]]] = {
    IO
      .parSequenceUnordered(requests.map(process(_).map(_.data)))
      .map(Ok(_))
  }

  def delete(id: UUID): IO[ErrorResponse, EmptyResponse] = {
    collection
      .deleteOne(id)
      .map(_ => NoContent.apply)
  }

  def delete(ids: List[UUID]): IO[ErrorResponse, Boolean] = {
    collection.deleteMany(ids)
  }
}

object TemplateModule {
  final case class TemplateBulkCreate(data: List[Template.Create])
}