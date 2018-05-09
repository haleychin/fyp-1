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
    // repo.getByEmail return a Future[Option[User]]
    // calling .map on it return a Option[User]
    val optionalResult = request.session.get("email").map(repo.getByEmail(_))

    optionalResult.map { futureResult =>
      futureResult.map{result =>
        result.map { u =>
          block(new AuthenticatedRequest(u, request))
        }
      }
    }

    Future.successful(Results.Redirect(routes.SessionController.newSession).flashing("error" -> "Please login first."))
  }
}

