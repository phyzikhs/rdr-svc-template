package com.radioretail.template.services.templates.routes

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.fullfacing.skyhook.core.protocol.RequestContext
import com.fullfacing.skyhook.macros.param.FromMap._
import com.fullfacing.skyhook.macros.param.RecordToMap._
import com.fullfacing.skyhook.rest.directives.JsonSchemaDirectives.schema
import com.fullfacing.skyhook.rest.directives.PatchDirectives.asJsonMergePatch
import com.fullfacing.skyhook.rest.directives.QueryDirectives.query
import com.fullfacing.skyhook.rest.directives.TaskDirectives.onComplete
import com.fullfacing.skyhook.rest.marshallers.Json4sSupport
import com.fullfacing.skyhook.rest.security.SecureHttpService
import com.fullfacing.skyhook.rest.unmarshallers.CsvListUnmarshaller.CsvList
import com.radioretail.common.authorisation.directives.DependencyAuthDirectives
import com.radioretail.template.domain.Template
import com.radioretail.template.handles.Json.formats
import com.radioretail.template.handles.Keycloak._
import com.radioretail.template.handles.Logging.logger
import com.radioretail.template.handles.Schedulers.fixed
import com.radioretail.template.services.templates.application.TemplateModule.TemplateBulkCreate
import com.radioretail.template.services.templates.application.{TemplateModule, TemplateValidators}
import io.opentracing.Tracer

final class TemplateRoutes()(implicit module: TemplateModule, tracer: Tracer)
  extends SecureHttpService("v2" / "templates")
    with DependencyAuthDirectives
    with Json4sSupport {

  val secureRoutes: RequestContext => Route = { implicit context =>
    get {
      path(JavaUUID) { id =>
        parameter("fields".as(CsvList[String]) ? List.empty[String]) { fields =>
          onComplete(module.fetchById(id, fields))
        }
      } ~
      pathEndOrSingleSlash {
        query[Template.Query](()) { q =>
          onComplete(module.queryL(q))
        }
      }
    } ~
    post {
      pathEndOrSingleSlash {
        schema[Template.Create](TemplateValidators.TemplateCreate) { body =>
          onComplete(module.create(body))
        }
      } ~
      path("bulk") {
        schema[TemplateBulkCreate](TemplateValidators.TemplateCreateMany) { body =>
          onComplete(module.createMany(body.data))
        }
      }
    } ~
    patch {
      path(JavaUUID) { id =>
        asJsonMergePatch(TemplateValidators.TemplateUpdate) { request =>
          onComplete(module.update(id, request))
        }
      }
    } ~
    delete {
      path(JavaUUID) { id =>
        onComplete(module.delete(id))
      }
    }
  }
}
