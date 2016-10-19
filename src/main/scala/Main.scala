package net.spantree.embeddings

import org.apache.spark.ml.feature.Word2VecModel
import org.apache.spark.sql.SparkSession

case class ModelRepo(
  words: Map[String,Seq[Double]],
  dimensions: Seq[(Int,Seq[(String,Double)])],
  model: Word2VecModel,
  spark: SparkSession
)

object Main {

  def main(args: Array[String]) {
    val spark: SparkSession = SparkSession.builder
        .master("local")
      .appName("WordEmbeddings")
      .getOrCreate

    val model = SparkJob(spark,"/Users/rwhaling/Downloads/brown/c*")

    val modelRepo = SparkJob.makeModelRepo(model,spark)

    for ((d,t) <- modelRepo.dimensions) {
      println(d)
      for ((w,t) <- t.take(5)) {
        println(w + ":" + t.toString)
      }
      println()
    }

    println("model built, initializing web server")

    val server = new Server(modelRepo,SchemaDef.WordEmbeddingSchema)
  }
}