/**
 * Code based on https://github.com/ttiurani/neo4j-scala
 */
package org.igorynia.neo4j.utils

import org.neo4j.graphdb.factory.GraphDatabaseFactory

trait GraphDatabaseServiceProvider {
  def ds: DatabaseService
}

trait ImplicitDatabaseProvider extends GraphDatabaseServiceProvider {
  implicit def ds: DatabaseService
}

trait EmbeddedGraphDatabaseServiceProvider extends GraphDatabaseServiceProvider {

  def graphDatabaseFactory: GraphDatabaseFactory

  def neo4jStoreDir: String

  def configParams = Map[String, String]()

  def nodeIndexConfig: List[(String, IndexCustomConfig)] = List()

  def relationIndexConfig: List[(String, IndexCustomConfig)] = List()

  lazy val ds: DatabaseService = {
    import scala.collection.JavaConverters._

    val database = graphDatabaseFactory
      .newEmbeddedDatabaseBuilder(neo4jStoreDir)
      .setConfig(configParams.asJava)
      .newGraphDatabase()

    DatabaseServiceImpl(database, nodeIndexConfig, relationIndexConfig)
  }

}

object SingletonProvider {

  private[neo4j] var ds: Option[DatabaseService] = None

  def apply(neo4jStoreDir: String,
            configParams: Map[String, String],
            nodeIndexParams: List[(String, IndexCustomConfig)],
            relationIndexParams: List[(String, IndexCustomConfig)]) = ds.getOrElse {
    import scala.collection.convert.WrapAsJava.mapAsJavaMap

    val database = new GraphDatabaseFactory()
      .newEmbeddedDatabaseBuilder(neo4jStoreDir)
      .setConfig(configParams)
      .newGraphDatabase()

    ds = Some(DatabaseServiceImpl(database, nodeIndexParams, relationIndexParams))

    sys.addShutdownHook(shutdown())

    ds.get
  }

  def shutdown(): Unit = {
    for (databaseService <- ds) {
      databaseService.gds.shutdown()
    }

    ds = None
  }

}

trait SingletonEmbeddedGraphDatabaseProvider extends GraphDatabaseServiceProvider {

  def neo4jStoreDir: String

  def configParams = Map[String, String]()

  def nodeIndexConfig: List[(String, IndexCustomConfig)] = List()

  def relationIndexConfig: List[(String, IndexCustomConfig)] = List()

  var ds: DatabaseService = SingletonProvider(neo4jStoreDir, configParams, nodeIndexConfig, relationIndexConfig)

  protected def shutdownDS(): Unit = SingletonProvider.shutdown()

}