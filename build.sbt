name         := "Word Embeddings"
version      := "1.0"
organization := "net.spantree"

scalaVersion := "2.11.8"

assemblyJarName in assembly := "uber.jar"

libraryDependencies ++=
  Seq("org.apache.spark" %% "spark-core" % "2.0.0" exclude("org.apache.ivy", "ivy"),
      "org.apache.spark" %% "spark-sql"  % "2.0.0" exclude("org.apache.ivy", "ivy"),
      "org.apache.spark" %% "spark-mllib"   % "2.0.0" exclude("org.apache.ivy", "ivy"),
	  "org.sangria-graphql" %% "sangria" % "0.7.0",
	  "org.sangria-graphql" %% "sangria-spray-json" % "0.3.1",
	  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % "2.4.11",
      "com.typesafe.akka" %% "akka-http-experimental" % "2.4.11")
resolvers ++= Seq(
 "scala-tools" at "https://oss.sonatype.org/content/groups/scala-tools",
 Resolver.mavenLocal,
 Resolver.sonatypeRepo("public"),
 Resolver.sonatypeRepo("snapshots"),
 Resolver.sonatypeRepo("releases"),
 "Akka Repository" at "http://repo.akka.io/releases/")

mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
  {
    case PathList("META-INF", xs @ _*) => MergeStrategy.discard
    case n if n.startsWith("reference.conf") => MergeStrategy.concat
    case x => MergeStrategy.first
  }
}
