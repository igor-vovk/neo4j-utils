package org.igorynia.neo4j.utils

import scala.collection.JavaConverters._

import org.igorynia.neo4j.utils.Transactional._
import org.neo4j.graphdb._

trait NodeDal[T <: NodeEntity] extends PropertyContainerAccess {

  import Neo4jWrapper._

  val NodeLabel: Label

  def hydrate(node: Node): Transactional[T] = for {
    checkedNode <- transactional(ds => checkNodeType(node, NodeLabel))
    hydrated <- doHydrate(checkedNode)
  } yield hydrated

  def persist(entity: T): Transactional[T] = for {
    node <- getOrCreateNode(entity.id, NodeLabel)
    persisted <- doPersist(entity, node)
  } yield persisted

  private def getOrCreateNode(maybeId: Option[Long], label: Label): Transactional[Node] = maybeId match {
    case Some(id) => getNodeById(id).map(checkNodeType(_, label))
    case None => createNodeWithLabel(label)
  }

  private def createNodeWithLabel(label: Label): Transactional[Node] = createNode.map { node =>
    node.addLabel(NodeLabel)

    node
  }

  def findById(id: Long): Transactional[Option[T]] =
    (catchNotFound opt getNodeById(id).flatMap(hydrate)).extract

  def extractNode(entity: Entity): Transactional[Option[Node]] = entity.id.map(getNodeById).extract

  def delete(entity: T): Transactional[Unit] =
    extractNode(entity).map(_.foreach(node => {
      node.getRelationships.asScala.foreach(_.delete())
      node.delete()
    }))

  private def checkNodeType(node: Node, label: Label): Node = {
    require(
      node.hasLabel(label),
      s"Can't hydrate node ${node.getId} to entity of type $NodeLabel"
    )

    node
  }

  protected def doHydrate(node: Node): Transactional[T]

  protected def doPersist(entity: T, node: Node): Transactional[T]

}

trait UnidirectionalRelationshipDal[StartEntity <: NodeEntity, EndEntity <: NodeEntity, Rel <: UnidirRelEntity[StartEntity, EndEntity]]
  extends PropertyContainerAccess with Neo4jWrapper {

  val RelationshipType: RelationshipType

  def findById(id: Long)(implicit aOp: NodeDal[StartEntity], bOp: NodeDal[EndEntity]) =
    catchNotFound.opt(getRelationshipById(id).flatMap(hydrate)).extract

  def find(start: StartEntity, end: EndEntity): Transactional[Option[Rel]] = for {
    startNodeO <- start.id.map(getNodeById).extract
    endNodeO <- end.id.map(getNodeById).extract
    relEntity <- (for {
      startNode <- startNodeO
      endNode <- endNodeO
      rel <- startNode.getRelationships(Direction.OUTGOING, RelationshipType).asScala.find(_.getEndNode == endNode)
    } yield hydrateHavingAll(rel, start, end)).extract
  } yield relEntity

  def getRelationship(entity: Entity): Transactional[Option[Relationship]] = entity.id.map(getRelationshipById).extract

  def hydrate(rel: Relationship)(implicit aOp: NodeDal[StartEntity], bOp: NodeDal[EndEntity]): Transactional[Rel] =
    for {
      a <- aOp.hydrate(rel.getStartNode)
      b <- bOp.hydrate(rel.getEndNode)
      relEntity <- hydrateHavingAll(rel, a, b)
    } yield relEntity

  def hydrateHavingStartEntity(rel: Relationship, a: StartEntity)(implicit bOp: NodeDal[EndEntity]): Transactional[Rel] =
    for {
      b <- bOp.hydrate(rel.getEndNode)
      relEntity <- hydrateHavingAll(rel, a, b)
    } yield relEntity

  def hydrateHavingEndEntity(rel: Relationship, b: EndEntity)(implicit aOp: NodeDal[StartEntity]): Transactional[Rel] =
    for {
      a <- aOp.hydrate(rel.getStartNode)
      relEntity <- hydrateHavingAll(rel, a, b)
    } yield relEntity

  def hydrateHavingAll(rel: Relationship, a: StartEntity, b: EndEntity): Transactional[Rel] =
    transactional(ds => doHydrate(checkRelType(rel), a, b))

  def persist(entity: Rel): Transactional[Rel] = {
    val (start, end) = (entity.start, entity.end)

    val rel = for {
      startNode <- getNodeById(start.id getOrElse entityNotPersisted)
      endNode <- getNodeById(end.id getOrElse entityNotPersisted)
    } yield startNode.getRelationships(Direction.OUTGOING, RelationshipType).asScala.find(_.getEndNode == endNode).getOrElse {
        startNode.createRelationshipTo(endNode, RelationshipType)
      }

    rel.flatMap(rel => transactional(doPersist(entity, rel)))
  }

  private def entityNotPersisted =
    throw new PersistenceException("Can't persist relationship, while at least one of the nodes are not persisted")

  private def checkRelType(rel: Relationship) = {
    if (rel.getType.name != RelationshipType.name()) {
      throw new BadRelationshipTypeException(
        s"Can't hydrate relationship ${rel.getId} of type ${rel.getType} to entity of type $RelationshipType"
      )
    }

    rel
  }

  def delete(entity: Rel): Transactional[Unit] = getRelationship(entity).map(_.foreach(_.delete()))

  protected def doHydrate(rel: Relationship, first: StartEntity, second: EndEntity): Rel

  protected def doPersist(entity: Rel, rel: Relationship)(ds: DatabaseService): Rel

}

trait HydrationException extends Exception

class BadRelationshipTypeException(message: String) extends HydrationException(message)

class MissingNodeLabelException(message: String) extends HydrationException(message)

class PersistenceException(message: String) extends HydrationException(message)