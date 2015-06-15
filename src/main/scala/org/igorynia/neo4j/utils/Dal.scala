package org.igorynia.neo4j.utils

import scala.collection.JavaConverters._

import org.neo4j.graphdb._

trait NodeDal[T <: NodeEntity] extends PropertyContainerAccess {

  import Neo4jWrapper._

  val NodeLabel: Label

  def hydrate(node: Node)(implicit ctx: TransactionContext): T = {
    doHydrate(checkNodeType(node, NodeLabel))
  }

  def persist(entity: T)(implicit ctx: TransactionContext) = {
    doPersist(entity, getOrCreateNode(entity.id, NodeLabel))
  }

  private def getOrCreateNode(maybeId: Option[Long], label: Label)(implicit ctx: TransactionContext): Node = {
    maybeId match {
      case Some(id) => checkNodeType(getNodeById(id), label)
      case None => createNodeWithLabel(label)
    }
  }

  private def createNodeWithLabel(label: Label)(implicit ctx: TransactionContext): Node = {
    val node = createNode()
    node.addLabel(NodeLabel)

    node
  }

  def findById(id: Long)(implicit ctx: TransactionContext): Option[T] = {
    catchNotFound opt hydrate(getNodeById(id))
  }

  def extractNode(entity: Entity)(implicit ctx: TransactionContext): Option[Node] = entity.id.map(getNodeById)

  def delete(entity: T)(implicit ctx: TransactionContext): Unit = {
    extractNode(entity).foreach(node => {
      node.getRelationships.asScala.foreach(_.delete())
      node.delete()
    })
  }

  private def checkNodeType(node: Node, label: Label): Node = {
    require(
      node.hasLabel(label),
      s"Can't hydrate node ${node.getId} to entity of type $NodeLabel"
    )

    node
  }

  protected def doHydrate(node: Node): T

  protected def doPersist(entity: T, node: Node): T

}

trait UnidirectionalRelationshipDal[StartEntity <: NodeEntity, EndEntity <: NodeEntity, Rel <: UnidirRelEntity[StartEntity, EndEntity]]
  extends PropertyContainerAccess with Neo4jWrapper {

  val RelationshipType: RelationshipType

  def findById(id: Long)(implicit ctx: TransactionContext, aOp: NodeDal[StartEntity], bOp: NodeDal[EndEntity]) = {
    catchNotFound.opt(hydrate(getRelationshipById(id)))
  }

  def find(start: StartEntity, end: EndEntity)(implicit ctx: TransactionContext): Option[Rel] = {
    val startNodeO = start.id.map(getNodeById)
    val endNodeO = end.id.map(getNodeById)

    for {
      startNode <- startNodeO
      endNode <- endNodeO
      rel <- startNode.getRelationships(Direction.OUTGOING, RelationshipType).asScala.find(_.getEndNode == endNode)
    } yield hydrateHavingAll(rel, start, end)
  }

  def getRelationship(entity: Entity)(implicit ctx: TransactionContext): Option[Relationship] = {
    entity.id.map(getRelationshipById)
  }

  def hydrate(rel: Relationship)(implicit ctx: TransactionContext, aOp: NodeDal[StartEntity], bOp: NodeDal[EndEntity]): Rel = {
    hydrateHavingAll(rel, aOp.hydrate(rel.getStartNode), bOp.hydrate(rel.getEndNode))
  }

  def hydrateHavingStartEntity(rel: Relationship, a: StartEntity)(implicit ctx: TransactionContext, bOp: NodeDal[EndEntity]): Rel = {
    hydrateHavingAll(rel, a, bOp.hydrate(rel.getEndNode))
  }

  def hydrateHavingEndEntity(rel: Relationship, b: EndEntity)(implicit ctx: TransactionContext, aOp: NodeDal[StartEntity]): Rel = {
    hydrateHavingAll(rel, aOp.hydrate(rel.getStartNode), b)
  }

  def hydrateHavingAll(rel: Relationship, a: StartEntity, b: EndEntity)(implicit ctx: TransactionContext): Rel = {
    doHydrate(checkRelType(rel), a, b)
  }

  def persist(entity: Rel)(implicit ctx: TransactionContext): Rel = {
    val (start, end) = (entity.start, entity.end)

    val startNode = getNodeById(start.id getOrElse entityNotPersisted)
    val endNode = getNodeById(end.id getOrElse entityNotPersisted)

    val rel = startNode
      .getRelationships(Direction.OUTGOING, RelationshipType)
      .asScala
      .find(_.getEndNode == endNode)
      .getOrElse(startNode.createRelationshipTo(endNode, RelationshipType))

    doPersist(entity, rel)
  }

  private def entityNotPersisted = {
    throw new PersistenceException("Can't persist relationship, while at least one of the nodes are not persisted")
  }

  private def checkRelType(rel: Relationship) = {
    if (rel.getType.name != RelationshipType.name()) {
      throw new BadRelationshipTypeException(
        s"Can't hydrate relationship ${rel.getId} of type ${rel.getType} to entity of type $RelationshipType"
      )
    }

    rel
  }

  def delete(entity: Rel)(implicit ctx: TransactionContext): Unit = {
    getRelationship(entity).foreach(_.delete())
  }

  protected def doHydrate(rel: Relationship, first: StartEntity, second: EndEntity)(implicit ctx: TransactionContext): Rel

  protected def doPersist(entity: Rel, rel: Relationship)(implicit ctx: TransactionContext): Rel

}

abstract class HydrationException(message: String) extends Exception(message)

class BadRelationshipTypeException(message: String) extends HydrationException(message)

class MissingNodeLabelException(message: String) extends HydrationException(message)

class PersistenceException(message: String) extends HydrationException(message)