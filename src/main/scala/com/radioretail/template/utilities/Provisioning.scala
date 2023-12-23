package com.radioretail.template.utilities

import cats.effect.Resource
import com.fullfacing.skyhook.json.serializers.JsonFormats.defaults
import com.radioretail.template.domain.Template
import monix.eval.Task
import org.json4s.jackson.JsonMethods

import scala.io.{BufferedSource, Source}

object Provisioning {

  /** Parses loaded JSON files into Template.Create models */
  private def parseToTemplates(): Task[List[Template.Create]] = {
    Task.parTraverseUnordered(Resources.all)(_.use { source =>
      Task {
        JsonMethods.parse(source.reader()).extract[List[Template.Create]]
      }
    }).map(_.flatten)
  }

  /**
   * Parses each listed JSON representations of Templates and saves it in the database
   * Replaces existing Templates (by name) to ensure all Templates are up to date
   */
  /*def initialize(): Task[Unit] = {
    implicit val ctx: RequestContext = RequestContext()

      for {
        templates <- parseToTemplates()
        results   <- Task.parTraverseUnordered(templates)(TemplateModule.replace())
      } yield results.foreach {
        case Left(ex) => logger.error(ex.toString)
        case _        => ()
      }
  }.onErrorHandleWithLogging(())*/

  private object Resources {
    private val account: Resource[Task, BufferedSource] = Resource.make {
      Task(Source.fromResource("templates/json/accountmessages.json"))
    } { source =>
      Task(source.close())
    }

    private val agency: Resource[Task, BufferedSource] = Resource.make {
      Task(Source.fromResource("templates/json/agencymessages.json"))
    } { source =>
      Task(source.close())
    }

    private val album: Resource[Task, BufferedSource] = Resource.make {
      Task(Source.fromResource("templates/json/albummessages.json"))
    } { source =>
      Task(source.close())
    }

    private val auth: Resource[Task, BufferedSource] = Resource.make {
      Task(Source.fromResource("templates/json/authmessages.json"))
    } { source =>
      Task(source.close())
    }

    private val client: Resource[Task, BufferedSource] = Resource.make {
      Task(Source.fromResource("templates/json/clientmessages.json"))
    } { source =>
      Task(source.close())
    }

    private val formatClock: Resource[Task, BufferedSource] = Resource.make {
      Task(Source.fromResource("templates/json/formatclockmessages.json"))
    } { source =>
      Task(source.close())
    }

    private val hardware: Resource[Task, BufferedSource] = Resource.make {
      Task(Source.fromResource("templates/json/hardwaremessages.json"))
    } { source =>
      Task(source.close())
    }

    private val job: Resource[Task, BufferedSource] = Resource.make {
      Task(Source.fromResource("templates/json/jobmessages.json"))
    } { source =>
      Task(source.close())
    }

    private val musicProfile: Resource[Task, BufferedSource] = Resource.make {
      Task(Source.fromResource("templates/json/musicprofilemessages.json"))
    } { source =>
      Task(source.close())
    }

    private val production: Resource[Task, BufferedSource] = Resource.make {
      Task(Source.fromResource("templates/json/productionmessages.json"))
    } { source =>
      Task(source.close())
    }

    private val product: Resource[Task, BufferedSource] = Resource.make {
      Task(Source.fromResource("templates/json/productmessages.json"))
    } { source =>
      Task(source.close())
    }

    private val schedule: Resource[Task, BufferedSource] = Resource.make {
      Task(Source.fromResource("templates/json/schedulemessages.json"))
    } { source =>
      Task(source.close())
    }

    private val store: Resource[Task, BufferedSource] = Resource.make {
      Task(Source.fromResource("templates/json/storemessages.json"))
    } { source =>
      Task(source.close())
    }

    private val storeList: Resource[Task, BufferedSource] = Resource.make {
      Task(Source.fromResource("templates/json/storelistmessages.json"))
    } { source =>
      Task(source.close())
    }

    private val track: Resource[Task, BufferedSource] = Resource.make {
      Task(Source.fromResource("templates/json/trackmessages.json"))
    } { source =>
      Task(source.close())
    }

    private val voiceOver: Resource[Task, BufferedSource] = Resource.make {
      Task(Source.fromResource("templates/json/voiceovermessages.json"))
    } { source =>
      Task(source.close())
    }

    val all: List[Resource[Task, BufferedSource]] = List(
      account,
      agency,
      auth,
      album,
      client,
      formatClock,
      hardware,
      job,
      musicProfile,
      production,
      product,
      schedule,
      store,
      storeList,
      track,
      voiceOver
    )
  }
}
