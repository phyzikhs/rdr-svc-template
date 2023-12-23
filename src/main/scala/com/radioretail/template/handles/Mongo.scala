package com.radioretail.template.handles

import com.fullfacing.skyhook.core.protocol.ErrorResponse
import com.fullfacing.skyhook.mongo.codecs.MongoCodecs
import com.fullfacing.skyhook.mongo.codecs.providers.EnumCodecProvider
import com.fullfacing.skyhook.mongo.database.MongoHealthCheck
import com.fullfacing.skyhook.mongo.formats.BsonFormats
import com.mongodb.connection.{ClusterConnectionMode, ClusterSettings}
import com.mongodb.{MongoClientSettings, ReadPreference}
import com.radioretail.common.utils.databases.code.IncrementedCode
import com.radioretail.common.utils.databases.masterelect.MasterElectRepository
import com.radioretail.template.enums.CommunicationTypes
import com.radioretail.template.services.templates.persistence.TemplateRepository
import io.github.cbartosiak.bson.codecs.jsr310.dayofweek.DayOfWeekAsStringCodec
import io.github.cbartosiak.bson.codecs.jsr310.offsettime.OffsetTimeAsStringCodec
import io.opentracing.contrib.mongo.common.TracingCommandListener
import io.opentracing.util.GlobalTracer
import monix.bio.{IO, UIO}
import monix.execution.Scheduler
import org.bson.UuidRepresentation
import org.bson.codecs.configuration.CodecProvider
import org.bson.codecs.configuration.CodecRegistries.{fromCodecs, fromProviders, fromRegistries}
import org.mongodb.scala.{MongoClient, MongoCredential, MongoDatabase, ServerAddress, WriteConcern}

import scala.jdk.CollectionConverters._

final class Mongo()
                 (implicit scheduler: Scheduler,
                  bsonFormats: BsonFormats)
  extends MongoHealthCheck {

  private val MONGODB_HOST = sys.env("MONGODB_HOST")
  private val MONGODB_PORT = sys.env("MONGODB_PORT").toInt
  private val MONGODB_USERNAME = sys.env("MONGODB_USERNAME")
  private val MONGODB_PASSWORD = sys.env("MONGODB_PASSWORD").toCharArray
  private val MONGODB_DATABASE = sys.env("MONGODB_DATABASE")

  // Cluster Addresses v
  private val addresses: List[ServerAddress] = List(ServerAddress(MONGODB_HOST, MONGODB_PORT))
  private val credentials = MongoCredential.createCredential(MONGODB_USERNAME, "admin", MONGODB_PASSWORD)

  // Cluster Settings v
  private val clusterSettings: ClusterSettings.Builder => ClusterSettings.Builder = { builder =>
    builder
      .hosts(addresses.asJava)
      .mode(ClusterConnectionMode.MULTIPLE)
      .addClusterListener(this)
  }

  private val providers: List[CodecProvider] = TemplateRepository.codecs :+ new EnumCodecProvider(CommunicationTypes)

  private val registries = List(
    fromProviders(providers.asJava),
    fromCodecs(new OffsetTimeAsStringCodec),
    fromCodecs(new DayOfWeekAsStringCodec)
  )

  private val mongoCodecs = MongoCodecs.from(_ => fromRegistries(registries.asJava))

  // Client Settings
  private val clientSettings: MongoClientSettings.Builder = MongoClientSettings.builder()
    .addCommandListener(new TracingCommandListener.Builder(GlobalTracer.get()).build())
    .retryWrites(true)
    .credential(credentials)
    .codecRegistry(mongoCodecs)
    .uuidRepresentation(UuidRepresentation.STANDARD)
    .applyToClusterSettings(s => clusterSettings(s))
    .writeConcern(WriteConcern.MAJORITY)
    .readPreference(ReadPreference.secondaryPreferred())

  // Client Instance
  private val client: MongoClient = MongoClient(clientSettings.build())
  private val database: MongoDatabase = client.getDatabase(MONGODB_DATABASE)

  val templateRepository: TemplateRepository          = new TemplateRepository(client, database)
  val masterElect: MasterElectRepository              = new MasterElectRepository(client, database)
  val code: IncrementedCode                           = new IncrementedCode(client, database)

  def shutdown(): UIO[Unit] = UIO {
    Logging.logger.info("Shutting down Mongo client...")
    client.close()
  }
}

object Mongo {

  /**
    * Provides an interface to initialize a connection to a an external resource.
    * This allows for controlled initialization of resources like database, message queue and cache connections.
    *
    * An object that represents a resource that should have controlled initialization should implement
    * this trait and provide an implementation.
    *
    * @return IO[ErrorResponse, Mongo] no error if connection success
    */
  def connect(implicit scheduler: Scheduler, bsonFormats: BsonFormats): IO[ErrorResponse, Mongo] = {
    val mongo = new Mongo()

    // TODO Look into potential search indexes
    UIO(mongo)
  }
}