package net.spantree.embeddings

import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.sql.Row
import org.apache.spark.ml.linalg.Vector
import org.apache.spark.ml.feature.Word2Vec
import org.apache.spark.ml.feature.Word2VecModel
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

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
    def sortVectorsByDimension(model:Word2VecModel, dimension:Integer, count:Integer):Array[(String,Double)] = model.getVectors
      .map { case Row(word:String,vector:org.apache.spark.ml.linalg.Vector) => (word,vector(dimension)) }
      .sort(desc("_2"))
      .take(count)

    /*
     * For a single word's vector representation, re-sort the dimensions by magnitude,
     * with dimensional indices attached, into an ordered list
     * Effectively, this produces the most-highly-ranked dimensions for a given word
     */
    def rankDimensions(vector:org.apache.spark.ml.linalg.Vector):Array[(Double,Int)] = 
      vector.toArray.zipWithIndex.sortBy(_._1)


    for (d <- 0 to model.getVectorSize - 1) {
      println(d)
      sortVectorsByDimension(model,d,20).foreach { println }
      println()
    }

  }
}