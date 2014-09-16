name := "neo4j-utils"

organization := "org.igorynia"

version := "0.2-SNAPSHOT"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

libraryDependencies += "org.neo4j" % "neo4j" % "2.1.1" % "provided"

libraryDependencies += "org.neo4j" % "neo4j-kernel" % "2.1.1" % "test" classifier "tests" classifier ""

// Publish settings
publishMavenStyle := true

publishArtifact in(Compile, packageBin) := true

publishArtifact in(Test, packageBin) := false

publishArtifact in(Compile, packageDoc) := true

publishArtifact in(Compile, packageSrc) := true

publishTo := Some(Resolver.file("file", new File(Path.userHome.absolutePath + "/maven/repo")))
