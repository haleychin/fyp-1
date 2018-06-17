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
case class ProfileData(name: String, email: String)

// User Controller
class UserController @Inject()(
  repo: UserRepository,
  authenticatedAction: AuthenticatedAction,
  cc: MessagesControllerComponents)
(implicit ec: ExecutionContext) extends
AbstractController(cc) with play.api.i18n.I18nSupport {

  // Define Form Structure
  val userForm = Form {
    mapping(
      "Name" -> nonEmptyText,
      "Email" -> email,
      "Password" -> nonEmptyText(minLength = 6)
    )(UserData.apply)(UserData.unapply)
  }
  val profileForm = Form {
    mapping(
      "Name" -> nonEmptyText,
      "Email" -> email
    )(ProfileData.apply)(ProfileData.unapply)
  }

  def newUser = Action { implicit request =>
    Ok(views.html.user.newUser(userForm))
  }

  def createUser = Action.async { implicit request =>
    userForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(Ok(views.html.user.newUser(errorForm)))
      },
      user => {
        val passwordHash = BCrypt.hashpw(user.password, BCrypt.gensalt());
        repo.create(user.name, user.email, passwordHash).map { result =>
          result match {
            case Failure(t) =>
              val errorForm = userForm.withError("email", "already been taken")
              Ok(views.html.user.newUser(errorForm))
            case Success(_) =>
              Redirect(routes.CourseController.index).flashing("success" -> "Successfully sign up and login.").withSession("email" -> user.email)
          }
        }

      }
    )
  }

  def showUser = authenticatedAction.async { implicit request =>
    repo.get(request.user.id).map { result =>
      result match {
        case Some(u) =>
          Ok(views.html.user.showUser(u))
        case None => Redirect(routes.PageController.index).flashing("error" -> "User not found.")
      }
    }
  }

  def editUser = authenticatedAction.async { implicit request =>
    repo.get(request.user.id).map { result =>
      result match {
        case Some(u) =>
          val filledForm = profileForm.fill(ProfileData(u.name, u.email))
          Ok(views.html.user.editUser(filledForm))
        case None => Redirect(routes.PageController.index).flashing("error" -> "User not found.")
      }
    }
  }

  def updateUser = authenticatedAction.async { implicit request =>
    profileForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(Ok(views.html.user.editUser(errorForm)))
      },
      user => {
        repo.update(request.user.id, user.name, user.email).map { result =>
          if (result > 0) {
            Redirect(routes.UserController.showUser)
          } else {
            Redirect(routes.UserController.editUser)
          }
        }
      }
    )
  }

  def deleteUser = authenticatedAction.async { implicit request =>
    repo.delete(request.user.id).map { _ =>
      Redirect(routes.PageController.index).flashing("success" -> "Successfully deleted your account.").withNewSession
    }
  }
}

