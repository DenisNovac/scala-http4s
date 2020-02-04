import cats.effect._, org.http4s._, org.http4s.dsl.io._, scala.concurrent.ExecutionContext.Implicits.global
import cats.implicits._
import org.http4s.server.blaze._
import org.http4s.implicits._
import org.http4s.server.Router


/** You don't need it if is inside IOApp */
implicit val cs: ContextShift[IO] = IO.contextShift(global)
implicit val timer: Timer[IO] = IO.timer(global)

/** Simple route returns just code 200 and text */
// An HttpRoutes[F] is a simple alias for Kleisli[OptionT[F, ?], Request, Response]
val helloWorldService = HttpRoutes.of[IO] {
  case GET -> Root / "hello" / name =>
    Ok(s"Hello, $name.")
}

/** If you want to return some Scala object - you'll
  * need to encode it. For this you must use encoder.
  */

/** Object that we want to return */
case class Tweet(id: Int, message: String)


/** Tweet encoder is explicitly used to encode Tweet
  * (explicitly because of EntityEncoder[IO, Tweet]).
  *
  * It will implicitly encoded in tweetService later
  * in this code: getTweet(tweetId).flatMap(Ok(_))
  *
  * NOTE: You'll need to use own encoder. For example, circe.
  * Then you'll write it like this: tweetEncoder = jsonOf[IO, Tweet]
  *
  * "jsonOf" is a method from "org.http4s.circe._" . Tweet must have somewhere circe
  * encoder such as implicit val tweetEncoder: Encoder[Tweet] = deriveEncoder[Tweet]
  */
implicit def tweetEncoder: EntityEncoder[IO, Tweet] = ???

/** Same here for code getPopularTweets().flatMap(Ok(_)) */
implicit def tweetsEncoder: EntityEncoder[IO, Seq[Tweet]] = ???


/** Those methods is not implemented because you can do anything here.
  * For example, take those objects from database or collection.
  */
def getTweet(tweetId: Int): IO[Tweet] = ???
def getPopularTweets(): IO[Seq[Tweet]] = ???

val tweetService = HttpRoutes.of[IO] {
  case GET -> Root / "tweets" / "popular" =>
    getPopularTweets().flatMap(Ok(_))
  case GET -> Root / "tweets" / IntVar(tweetId) =>
    getTweet(tweetId).flatMap(Ok(_))
}


/** Running the service */


/** Combining multiple HTTP routes */
val services = tweetService <+> helloWorldService
val httpApp = Router("/" -> helloWorldService, "/api" -> services).orNotFound
val serverBuilder = BlazeServerBuilder[IO].bindHttp(8080, "localhost").withHttpApp(httpApp)

/** Run server */
val fiber = serverBuilder.resource.use(_ => IO.never).start.unsafeRunSync()
/** Stop server (comment if in REPL and uncomment when need to stop) */
fiber.cancel.unsafeRunSync()


/** Example:
  *
  * curl http://localhost:8080/hello/Pete
  * Hello, Pete.
  */



