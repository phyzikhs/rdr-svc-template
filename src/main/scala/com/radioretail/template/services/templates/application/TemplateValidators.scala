package com.radioretail.template.services.templates.application

import cats.effect.Resource
import com.fullfacing.skyhook.json.schema.{JsonObjectSchema, JsonSchema}
import com.radioretail.template.domain.Template
import com.radioretail.template.services.templates.application.TemplateModule.TemplateBulkCreate
import monix.bio.Task

import scala.io.{BufferedSource, Source}

object TemplateValidators {

  object TemplateCreate extends JsonObjectSchema[Template.Create] {
    override val source: Resource[Task, BufferedSource] = Resource.make {
      Task(Source.fromResource("schemas/create.json"))
    } { source =>
      Task(source.close())
    }
  }

  object TemplateCreateMany extends JsonObjectSchema[TemplateBulkCreate] {
    override val source: Resource[Task, BufferedSource] = Resource.make {
      Task(Source.fromResource("schemas/create_many.json"))
    } { source =>
      Task(source.close())
    }
  }

  object TemplateUpdate extends JsonObjectSchema[Template.Update] {
    override val source: Resource[Task, BufferedSource] = Resource.make {
      Task(Source.fromResource("schemas/update.json"))
    } { source =>
      Task(source.close())
    }
  }
}
