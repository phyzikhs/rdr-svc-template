package com.radioretail.template.handles

import com.radioretail.common.serialization.SerializationFormats.common
import com.radioretail.template.serialization.TemplateSerialization
import org.json4s.Formats

object Json {
  implicit val formats: Formats = common ++ TemplateSerialization.all
}
