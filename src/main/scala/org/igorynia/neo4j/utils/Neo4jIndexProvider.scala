package org.igorynia.neo4j.utils

import scala.collection.mutable
import org.neo4j.graphdb.{PropertyContainer, Node}
import org.neo4j.graphdb.index.{RelationshipIndex, Index}
import scala.collection.convert.WrapAsJava._
import scala.language.implicitConversions

/**
 * Provides Index access trait
 * @author Christopher Schmidt
 */
trait Neo4jIndexProvider {

  def ds: DatabaseService

  def getIndexManager = ds.gds.index()

  private lazy val nodeIndexStore: mutable.Map[String, Index[Node]] = {
    mutable.Map() ++= ds.nodeIndexConfig.map((initNodeIndex _).tupled)
  }

  private lazy val relationIndexStore: mutable.Map[String, RelationshipIndex] = {
    mutable.Map() ++= ds.relationIndexConfig.map((initRelationshipIndex _).tupled)
  }

  def getNodeIndex(name: String): Index[Node] = nodeIndexStore(name)

  def getRelationIndex(name: String) = relationIndexStore(name)

  private def initNodeIndex(indexName: String, config: IndexCustomConfig): (String, Index[Node]) = {
    val m = getIndexManager
    indexName -> config.map(mapAsJavaMap).fold(m.forNodes(indexName))(m.forNodes(indexName, _))
  }

  private def initRelationshipIndex(indexName: String, config: IndexCustomConfig): (String, RelationshipIndex) = {
    val m = getIndexManager
    indexName -> config.map(mapAsJavaMap).fold(m.forRelationships(indexName))(m.forRelationships(indexName, _))
  }

  def recreateNodeIndex(name: String) {
    this.synchronized {
      ds.nodeIndexConfig.toMap.get(name).foreach(config => {
        getNodeIndex(name).delete()

        nodeIndexStore += initNodeIndex(name, config)
      })
    }
  }

  class IndexWrapper[T <: PropertyContainer](i: Index[T]) {
    def +=(t: T)(kv: Pair[String, Any]) = {
      i.add(t, kv._1, kv._2)
      t
    }

    def ++=(t: T)(kv: Seq[Pair[String, Any]]) = kv.foreach(+=(t))

    def -=(t: T, k: String, v: Any) = i.remove(t, k, v)

    def -=(t: T, k: String) = i.remove(t, k)

    def -=(t: T) = i.remove(t)
  }

  implicit def indexToRichIndex[T <: PropertyContainer](i: Index[T]) = new IndexWrapper[T](i)

}

object Neo4jIndexProvider {
  def apply(_ds: DatabaseService) = new Neo4jIndexProvider {
    override def ds = _ds
  }
}