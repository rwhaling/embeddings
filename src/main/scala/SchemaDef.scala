package net.spantree.embeddings

import sangria.schema._

object SchemaDef {
  val NumberOfWordsArg = Argument(
    "numberOfWords", IntType)

  val WordArg = Argument(
    "word", StringType)

  val DimensionArg = Argument(
    "dimension", IntType)

  val WordVector = ObjectType(
    "WordVector",
    "a single word's vector of weights in each dimension",
    fields[ModelRepo,(String,Seq[Double])](
      Field("word",StringType,
        resolve = _.value._1),
      Field("vector",ListType(FloatType),
        resolve = _.value._2),
      Field("vectorSize",IntType,
        resolve = _.value._2.length),
      Field("synonyms",ListType(StringType),
        arguments = NumberOfWordsArg :: Nil,
        resolve = (context) => {
          val (modelRepo,word,n) = (context.ctx, context.value._1, context.arg(NumberOfWordsArg))
          val model = modelRepo.model
          val spark = modelRepo.spark
          SparkJob.findSynonyms(model,word,n,spark)
        }
      )
    )
  )

  val WeightedWord = ObjectType(
    "WeightedWord",
    "a single ranked word in a dimension of a vector",
    fields[ModelRepo,(String,Double)](
      Field("weight",FloatType,
        resolve = _.value._2),
      Field("word",WordVector,
        resolve = (ctx) => (ctx.value._1,ctx.ctx.words.getOrElse(ctx.value._1,List())))
    )
  )

  val Dimension = ObjectType(
    "Dimension",
    "a single dimension of the vector",
    fields[Unit,(Int,Seq[(String,Double)])](
      Field("n",IntType,
        resolve = _.value._1 ),
      Field("words",ListType(WeightedWord),
        arguments = NumberOfWordsArg :: Nil,
        resolve = (ctx) => ctx.value._2.slice(0,ctx.arg(NumberOfWordsArg)))
    )
  )

  val Query = ObjectType(
    "Query",
    "query by words or dimensions",
    fields[ModelRepo, Unit](
      Field("words", ListType(WordVector),
        arguments = NumberOfWordsArg :: Nil,
        resolve = (ctx) => ctx.ctx.words.toSeq.slice(0,ctx.arg(NumberOfWordsArg))),
      Field("word", WordVector,
        arguments = WordArg :: Nil,
        resolve = (ctx) => (ctx.arg(WordArg),ctx.ctx.words(ctx.arg(WordArg)))),
      Field("dimensions", ListType(Dimension),
        resolve = _.ctx.dimensions),
      Field("dimension", Dimension,
        arguments = DimensionArg :: Nil,
        resolve = (ctx) => ctx.ctx.dimensions(ctx.arg(DimensionArg)))
    )
  )

  val WordEmbeddingSchema = Schema(Query)
}