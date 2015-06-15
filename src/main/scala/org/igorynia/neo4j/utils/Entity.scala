package org.igorynia.neo4j.utils

import org.neo4j.graphdb.PropertyContainer

trait Entity {

  def id: Option[Long]

  def same(other: Entity) = id.contains(other.id.getOrElse(-1L))

}

trait NodeEntity extends Entity

/**
 * Unidirectional relationship entity
 * @tparam StartEntity entity, from which relationship starts
 * @tparam EndEntity entity, in which relationship ends
 */
trait UnidirRelEntity[StartEntity <: NodeEntity, EndEntity <: NodeEntity] extends Entity {

  def start: StartEntity

  def end: EndEntity

}

trait PropertyContainerAccess {

  def getProp[T](name: String)(implicit pc: PropertyContainer): T = pc.getProperty(name).asInstanceOf[T]

  def getPropOpt[T](name: String)(implicit pc: PropertyContainer): Option[T] = Option(pc.getProperty(name, null).asInstanceOf[T])

  def removeProp(name: String)(implicit pc: PropertyContainer): Unit = pc.removeProperty(name)

  def setProp(name: String, value: Any)(implicit pc: PropertyContainer): Unit = value match {
    case Some(innerVal) => setProp(name, innerVal)
    case None => removeProp(name)
    case _ => pc.setProperty(name, value)
  }

}

object PropertyContainerAccess extends PropertyContainerAccess