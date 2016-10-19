package net.spantree.embeddings
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import akka.http.scaladsl.model.StatusCodes._
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import org.apache.spark.ml.feature.{Word2VecModel, Word2Vec}
import org.apache.spark.sql.SparkSession
import sangria.parser.QueryParser
import sangria.execution.{ErrorWithResolver, QueryAnalysisError, Executor}
import sangria.marshalling.sprayJson._
import sangria.schema.Schema
import spray.json._
import scala.util.{Success, Failure}

object Server {
  def main(args: Array[String]) {
    println("opening spark context")
    val spark: SparkSession = SparkSession.builder
      .master("local")
      .appName("WordEmbeddings")
      .getOrCreate
    println("loading model")

    import spark.implicits._

    val model = Word2VecModel.load(args(0))
    println("model loaded")
    val modelRepo = SparkJob.makeModelRepo(model,spark)

    println("serving on http://localhost:8080")
    println("browse to http://localhost:8080/graphiql.html to query")
    val server = new Server(modelRepo, SchemaDef.WordEmbeddingSchema)
  }
}

class Server[Ctx](model:Ctx,schema:Schema[Ctx,Unit]) {


  implicit val system = ActorSystem("sangria-server")
  implicit val materializer = ActorMaterializer()

  import system.dispatcher

  val route =
    (post & path("graphql")) {
      entity(as[JsValue]) { requestJson ⇒
        val JsObject(fields) = requestJson

        val JsString(query) = fields("query")

        val operation = fields.get("operationName") collect {
          case JsString(op) ⇒ op
        }

        val vars = fields.get("variables") match {
          case Some(obj: JsObject) ⇒ obj
          case Some(JsString(s)) if s.trim.nonEmpty ⇒ s.parseJson
          case _ ⇒ JsObject.empty
        }
        val parsed = QueryParser.parse(query)

        QueryParser.parse(query) match {

          // query parsed successfully, time to execute it!
          case Success(queryAst) ⇒
            complete(Executor.execute(schema, queryAst, model)
              .map(OK → _)
              .recover {
                case error: QueryAnalysisError ⇒ BadRequest → error.resolveError
                case error: ErrorWithResolver ⇒ InternalServerError → error.resolveError
              })
          // can't parse GraphQL query, return error
          case Failure(error) ⇒
            complete(BadRequest, JsObject("error" -> JsString(error.getMessage)))
        }
      }
    } ~
    get {
      getFromResourceDirectory("")
    }

  val binding = Http().bindAndHandle(route, "0.0.0.0", 8080)

}