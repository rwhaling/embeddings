package net.spantree.embeddings

import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.sql.Row
import org.apache.spark.ml.linalg.Vector
import org.apache.spark.ml.feature.Word2Vec
import org.apache.spark.ml.feature.Word2VecModel
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

object SparkApp {

  def main(args: Array[String]) {
    val spark: SparkSession = SparkSession.builder
      .appName("WordEmbeddings")
      .getOrCreate

    import spark.implicits._

    val files = spark.sparkContext.wholeTextFiles("/Users/rwhaling/Downloads/brown/c*")

    /*
     * Load the files, and split on newlines.  Each file has one sentence per line.
     */
    val lines = files
      .flatMap { case (fn,content) => content.split("\\n+") } 
      .filter { line => !line.equals("") } // Remove empty lines

    /*
     * We want to split our sentences into words, but each token in the corpus also has POS tags.
     * So we'll split on / to remove the tag, and only take the word.
     */
    val sentences = lines
      .map { line => line.split("\\s+")
      .map { _.split("/")
      .take(1)(0) } } 
      .toDF("text")


    /*
     * Build and train a Word2Vec model of 100 dimensions.
     */
    val word2Vec = new Word2Vec()
      .setInputCol("text")
      .setOutputCol("result")
      .setVectorSize(100)
      .setMinCount(0)

    val model = word2Vec.fit(sentences)

    model.getVectors.show

    /*
     * For a given dimension, sort the (word,vector) rows in the model by the value of that dimension
     * Effectively, this produces the most-highly-ranked words for a given dimension
     */
    def sortVectorsByDimension(vectors:Array[Row], dimension:Integer, count:Integer):Array[(String,Double)] = vectors
      .map { case Row(word:String,vector:org.apache.spark.ml.linalg.Vector) => (word,vector(dimension)) }
      .sortBy(_._2)
      .take(count)

      // .sort(desc("_2"))

    /*
     * For a single word's vector representation, re-sort the dimensions by magnitude,
     * with dimensional indices attached, into an ordered list
     * Effectively, this produces the most-highly-ranked dimensions for a given word
     */
    def rankDimensions(vector:org.apache.spark.ml.linalg.Vector):Array[(Double,Int)] = 
      vector.toArray.zipWithIndex.sortBy(_._1)

    val mod = spark.sparkContext.broadcast(model)
    val vectors = model.getVectors.collect()

    val topWords = (0 to model.getVectorSize - 1).map { d =>
      (d,sortVectorsByDimension(vectors,d,50))
    }

    for ((d,t) <- topWords) {
      println(d)
      for ((w,t) <- t) {
        println(w + ":" + t.toString)
      }
      println()
    }

    println("model built, initializing web server")

    implicit val system = ActorSystem("sangria-server")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher

    import akka.http.scaladsl.server.Route._

    val route = 
      path("hello") {
        get {
          complete("Say hello to akka-http")
        }
      }

    Http().bindAndHandle(route, "0.0.0.0", 8080)
  }
}