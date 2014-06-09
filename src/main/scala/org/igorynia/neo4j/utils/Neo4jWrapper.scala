package org.igorynia.neo4j.utils

import java.util

import scala.collection.JavaConverters._
import scala.collection.convert.WrapAsScala._
import scala.util.control.Exception

import Transactional._
import org.neo4j.graphdb._
import org.neo4j.graphdb.index.{Index, UniqueFactory}
import org.neo4j.graphdb.index.UniqueFactory.UniqueNodeFactory

trait Neo4jWrapper {

  def createNode: Transactional[Node] = transactional(_.gds.createNode())

  val catchNotFound = Exception.catching(classOf[NotFoundException])

  def getNodeById(id: Long): Transactional[Node] = transactional(_.gds.getNodeById(id))

  def getRelationshipById(id: Long): Transactional[Relationship] = transactional(_.gds.getRelationshipById(id))

  def buildUniqueFactory(index: Index[Node]): UniqueFactory[Node] = new UniqueNodeFactory(index) {
    def initialize(created: Node, properties: util.Map[String, AnyRef]) {
      mapAsScalaMap(properties).foreach {
        case (key, value) => created.setProperty(key, value)
      }
    }
  }

}

object Neo4jWrapper extends Neo4jWrapper

private[neo] class RichPropertyContainer(pc: PropertyContainer) extends scala.collection.mutable.Map[String, Any] {

  def get(key: String): Option[Any] = {
    if (pc.hasProperty(key)) Some(pc.getProperty(key))
    else None
  }

  def iterator: Iterator[(String, Any)] = pc.getPropertyKeys.iterator.map {
    case key => key -> pc.getProperty(key)
  }

  def +=(kv: (String, Any)) = {
    pc.setProperty(kv._1, kv._2)
    this
  }

  def -=(key: String) = {
    if (pc.hasProperty(key)) pc.removeProperty(key)
    this
  }
}

private[neo] class NodeRelationshipMethods(node: Node, rel: Relationship = null) {

  def -->(relType: RelationshipType) = new OutgoingRelationshipBuilder(node, relType)

  def <--(relType: RelationshipType) = new IncomingRelationshipBuilder(node, relType)

  /**
   * use this to get the created relationship object
   * <pre>start --> "KNOWS" --> end <()</pre>
   */
  def <() = rel

}

private[neo] class OutgoingRelationshipBuilder(fromNode: Node, relType: RelationshipType) {

  def -->(toNode: Node) = {
    new NodeRelationshipMethods(toNode, fromNode.createRelationshipTo(toNode, relType))
  }

  def <() = Option(fromNode.getSingleRelationship(relType, Direction.OUTGOING))

  def <<() = fromNode.getRelationships(Direction.OUTGOING, relType).asScala

}

private[neo] class IncomingRelationshipBuilder(toNode: Node, relType: RelationshipType) {

  def <--(fromNode: Node) = {
    new NodeRelationshipMethods(fromNode, fromNode.createRelationshipTo(toNode, relType))
  }

  def <() = Option(toNode.getSingleRelationship(relType, Direction.INCOMING))

  def <<() = toNode.getRelationships(Direction.INCOMING, relType).asScala

}