package net.spantree.embeddings

import org.apache.spark.ml.feature.{Word2Vec, Word2VecModel}
import org.apache.spark.sql.{Row, SparkSession}

object SparkJob {
  def apply(spark: SparkSession, filePath: String): Word2VecModel = {
    import spark.implicits._

    val files = spark.sparkContext.wholeTextFiles(filePath)

    /*
     * Load the files, and split on newlines.  Each file has one sentence per line.
     */
    val lines = files
      .flatMap { case (fn, content) => content.split("\\n+") }
      .filter { line => !line.equals("") } // Remove empty lines

    /*
     * We want to split our sentences into words, but each token in the corpus also has POS tags.
     * So we'll split on / to remove the tag, and only take the word.
     */
    val sentences = lines
      .map { line => line.split("\\s+")
        .map {
          _.split("/")
            .take(1)(0)
        }
      }
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

    model
  }

  /*
   * For a given dimension, sort the (word,vector) rows in the model by the value of that dimension
   * Effectively, this produces the most-highly-ranked words for a given dimension
   */
  def sortVectorsByDimension(vectors: Array[Row], dimension: Integer, count: Integer): Seq[(String, Double)] = vectors
    .map { case Row(word: String, vector: org.apache.spark.ml.linalg.Vector) => (word, vector(dimension)) }
    .sortBy(_._2)
    .take(count)

  /*
   * For a single word's vector representation, re-sort the dimensions by magnitude,
   * with dimensional indices attached, into an ordered list
   * Effectively, this produces the most-highly-ranked dimensions for a given word
   */
  def rankDimensions(vector: org.apache.spark.ml.linalg.Vector): Array[(Double, Int)] =
    vector.toArray.zipWithIndex.sortBy(_._1)

  /*
   * Helper method to create a ModelRepo from the Model
   */
  def makeModelRepo(model: Word2VecModel,spark:SparkSession): ModelRepo = {
    val vectors = model.getVectors.collect()

    val vectorRepo = vectors
      .map { case Row(word: String, vector: org.apache.spark.ml.linalg.Vector) =>
        word -> vector.toArray.toSeq
      }.toMap

    val dimensions = (0 until model.getVectorSize)
      .zipWithIndex
      .map { case (d, index) =>
        (index, sortVectorsByDimension(vectors, d, 50))
      }

    ModelRepo(vectorRepo, dimensions,model,spark)
  }

  def findSynonyms(model:Word2VecModel,word:String,n:Int,spark:SparkSession): Seq[String] = {
    import spark.implicits._
    try {
      model.findSynonyms(word, n).map {
        case Row(word: String, similarity: Double) => word
      }.collect()
    } catch {
      case e:Exception => List()
    }
  }

  def main(args: Array[String]) {
    println("opening spark context")
    val spark: SparkSession = SparkSession.builder
      .master("local")
      .appName("WordEmbeddings")
      .getOrCreate
    println("fitting model")
    val model = SparkJob(spark,"/Users/rwhaling/Downloads/brown/c*")
    println("saving model")
    model.write.overwrite().save("model.json")
    println ("done")

  }
}