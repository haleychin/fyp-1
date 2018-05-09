package controllers

import javax.inject._
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Failure}

// For Form
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

// Model
import models._

// BCrypt
import org.mindrot.jbcrypt.BCrypt

// Define Form Case Class
case class UserData(name: String, email: String, password: String)

// User Controller
class UserController @Inject()(
  repo: UserRepository,
  cc: MessagesControllerComponents)
(implicit ec: ExecutionContext) extends
MessagesAbstractController(cc) {

  // Define Form Structure
  val userForm = Form {
    mapping(
      "name" -> nonEmptyText,
      "email" -> email,
      "password" -> nonEmptyText(minLength = 6)
    )(UserData.apply)(UserData.unapply)
  }

  def index = Action.async { implicit request =>
    val optionalResult = request.session.get("email").map(repo.getByEmail(_))
    optionalResult match {
      case Some(futureResult) =>
        futureResult.map { result =>
          result match {
            case Some(u) => Ok(views.html.user.index(u))
            case None => Redirect(routes.SessionController.newSession).flashing("error" -> "Please login first.")
          }
        }
      case None =>
        Future.successful(Redirect(routes.SessionController.newSession).flashing("error" -> "Please login first."))
    }
  }

  def newUser = Action { implicit request =>
    Ok(views.html.user.newUser(userForm))
  }

  def createUser = Action.async { implicit request =>
    userForm.bindFromRequest.fold(
      // If Error
      errorForm => {
        Future.successful(Ok(views.html.user.newUser(errorForm)))
      },
      // Success
      user => {
        val passwordHash = BCrypt.hashpw(user.password, BCrypt.gensalt());
        repo.create(user.name, user.email, passwordHash).map { result =>
          result match {
            case Failure(t) =>
              val errorForm = userForm.withError("email", "already been taken")
              Ok(views.html.user.newUser(errorForm))
            case Success(_) =>
              Redirect(routes.UserController.index).flashing("success" -> "Succesfully sign up")
          }
        }

      }
    )
  }

}

