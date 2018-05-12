package controllers

import javax.inject.Inject
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future, Await}
import scala.concurrent.duration._
import scala.util.{Success, Failure}

import models._

class AuthenticatedRequest[A](val user: User, val request: Request[A])
  extends WrappedRequest[A](request)

class AuthenticatedAction  @Inject()(
  val parser: BodyParsers.Default,
  repo: UserRepository)
(implicit val executionContext: ExecutionContext) extends
ActionBuilder[AuthenticatedRequest, AnyContent] {

  def invokeBlock[A](
      request: Request[A],
      block: AuthenticatedRequest[A] => Future[Result]): Future[Result] = {
    val optionalEmail = request.session.get("email")

    optionalEmail match {
      case Some(email) =>
        repo.getByEmail(email).map { result =>
          result match {
            case Some(u) =>
              Await.result(block(new AuthenticatedRequest(u, request)), 1 second)
            case None => Results.Redirect(routes.SessionController.newSession).flashing("error" -> "Please login first.")
          }
        }
      case None => Future.successful(Results.Redirect(routes.SessionController.newSession).flashing("error" -> "Please login first."))
    }

  }

}

