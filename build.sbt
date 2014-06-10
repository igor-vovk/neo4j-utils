name := "neo4j-utils"

organization := "org.igorynia"

version := "0.1-SNAPSHOT"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies += "org.neo4j" % "neo4j" % "2.1.1" % "provided"

libraryDependencies += "org.neo4j" % "neo4j-kernel" % "2.1.1" % "test" classifier "tests" classifier ""