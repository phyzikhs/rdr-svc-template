package com.radioretail.template.services.templates.application.engine

import de.zalando.beard.renderer.RenderResult

class StringRenderResult extends RenderResult[String] {
  val sb = new StringBuilder

  override def write(string: String): Unit = sb.append(string)

  override def result: String = sb.toString()
}

object StringRenderResult {
  def apply(): StringRenderResult = new StringRenderResult()
}