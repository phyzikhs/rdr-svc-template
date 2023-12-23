package com.radioretail.template.services.templates.persistence

import cats.implicits._
import com.fullfacing.skyhook.core.NotFound
import com.fullfacing.skyhook.core.protocol.ErrorResponse
import com.fullfacing.skyhook.mongo.database.DocumentStore.DeleteStrategy
import com.fullfacing.skyhook.mongo.database._
import com.fullfacing.skyhook.mongo.formats.BsonFormats
import com.mongodb.client.model.ReplaceOptions
import com.radioretail.common.utils.databases.query.OrQuery
import com.radioretail.template.domain.{Template, TemplateRequest}
import monix.bio.IO
import monix.execution.Scheduler
import org.bson.codecs.configuration.CodecProvider
import org.mongodb.scala.bson.codecs.Macros
import org.mongodb.scala.model.Filters._
import org.mongodb.scala.{MongoClient, MongoDatabase}

import java.util.UUID


final class TemplateRepository(client: MongoClient, database: MongoDatabase)
                              (implicit scheduler: Scheduler,
                               val bsonFormats: BsonFormats)
  extends DocumentStore[Template](client, database)
    with Query[Template, Template.Query, Template.Fetch]
    with OrQuery[Template, Template.Query, Template.Fetch]
    with MongoJsonPatch[Template, Template.Update]
    with MongoMergePatch[Template]
    with Status[Template] {

  override protected val COLLECTION_NAME: String = "templates"
  override protected val delete: DeleteStrategy  = DeleteStrategy.Soft

  override val localStatusOnly: Boolean = false

  //override implicit val bsonFormats: BsonFormats = DefaultBsonFormats

  private val replaceOptions: ReplaceOptions = new ReplaceOptions().upsert(true)

  def fetchOne(req: TemplateRequest): IO[ErrorResponse, Template] = {
    val filter =
      and(
        equal("name", req.name),
        equal("type", req.`type`),
        equal("languageCode", req.languageCode)
      )

    IO.fromOptionEval(
      findOne(filter),
      NotFound(s"${req.`type`} template with name: ${req.name} not found.")
    )
  }

  def updateOne(id: UUID, update: Template.Update): IO[ErrorResponse, Unit] = ???

  /**
    * Replaces a Template by name and type. If no Template with the given exists it will be inserted instead.
    *
    * @param template The Template to be replaced/inserted
    * @return Unit
    */
  def replace(template: Template): IO[ErrorResponse, Unit] = {
    val filter = and(equal("name", template.name), equal("type", template.`type`))

    IO.fromEither {
      collection
        .replaceOne(filter, template, replaceOptions)
        .asRight
        .map(_ => ())
    }
  }
}

object TemplateRepository {
  val codecs: List[CodecProvider] = {
    import Template.{Fetch, Update}
    List(
      Macros.createCodecProviderIgnoreNone[Template],
      Macros.createCodecProviderIgnoreNone[Update],
      Macros.createCodecProviderIgnoreNone[Fetch]
    )
  }
}