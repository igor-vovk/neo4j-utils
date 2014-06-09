package org.igorynia.neo4j.utils

import org.neo4j.graphdb.{DynamicLabel, Node, DynamicRelationshipType, PropertyContainer}

import scala.language.implicitConversions

/**
 * trait for implicits
 * used by Neo4j wrapper
 *
 * @author Christopher Schmidt
 */
trait Neo4jWrapperImplicits {

  implicit def node2RelationshipBuilder(node: Node) = new NodeRelationshipMethods(node)

  implicit def string2RelationshipType(relType: String) = DynamicRelationshipType.withName(relType)
  
  implicit def string2NodeLabel(label: String) = DynamicLabel.label(label)

  implicit def propertyContainerToRichPropertyContainer(propertyContainer: PropertyContainer) = {
    new RichPropertyContainer(propertyContainer)
  }

  implicit def indexManager(implicit ds: DatabaseService) = ds.gds.index()

}

object Neo4jWrapperImplicits extends Neo4jWrapperImplicits