package com.radioretail.template.services.templates.application.engine

import de.zalando.beard.filter.Filter

class TableData extends Filter {
  override def name: String = "TableData"

  override def apply(value: String, parameters: Map[String, Any]): String = value

  override def applyIterable(value: Iterable[_], parameters: Map[String, Any]): Iterable[String] = {
    value.map("\n" + _.asInstanceOf[List[String]].mkString(","))
  }
}

object TableData {
  def apply(): TableData = new TableData()
}