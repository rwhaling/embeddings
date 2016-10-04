name         := "Word Embeddings"
version      := "1.0"
organization := "net.spantree"

scalaVersion := "2.11.8"

libraryDependencies ++= 
  Seq("org.apache.spark" %% "spark-core" % "2.0.0",   
      "org.apache.spark" %% "spark-sql"  % "2.0.0",
      "org.apache.spark" %% "spark-mllib"   % "2.0.0")
resolvers += Resolver.mavenLocal