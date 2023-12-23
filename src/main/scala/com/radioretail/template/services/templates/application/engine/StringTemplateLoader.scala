package com.radioretail.template.services.templates.application.engine

import de.zalando.beard.renderer.{TemplateLoader, TemplateName}

import scala.util.Try

class StringTemplateLoader(template: String) extends TemplateLoader {
  override def load(templateName: TemplateName): Try[String] = {
    Try(template)
  }
}

object StringTemplateLoader {
  def apply(template: String): StringTemplateLoader = new StringTemplateLoader(template)
}