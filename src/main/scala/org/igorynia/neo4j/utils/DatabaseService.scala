package org.igorynia.neo4j.utils

import org.neo4j.graphdb.GraphDatabaseService

/**
 * Interface for GraphDatabaseService
 * @author Christopher Schmidt
 */
trait DatabaseService {

  def gds: GraphDatabaseService

  def nodeIndexConfig: List[(String, IndexCustomConfig)]

  def relationIndexConfig: List[(String, IndexCustomConfig)]

}

/**
 * Default implementation
 */
case class DatabaseServiceImpl(gds: GraphDatabaseService,
                               nodeIndexConfig: List[(String, IndexCustomConfig)],
                               relationIndexConfig: List[(String, IndexCustomConfig)]
                                ) extends DatabaseService
