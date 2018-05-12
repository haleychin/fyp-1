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
      "name" -> nonEmptyText,
      "email" -> email,
      "password" -> nonEmptyText(minLength = 6)
    )(UserData.apply)(UserData.unapply)
  }
  val profileForm = Form {
    mapping(
      "name" -> nonEmptyText,
      "email" -> email
    )(ProfileData.apply)(ProfileData.unapply)
  }

  // def authenticate[A](action: Action[A]) = Action.async(action.parser) { request =>
  //   val optionalResult = request.session.get("email").map(repo.getByEmail(_))

  //   optionalResult match {
  //     case Some(futureResult) =>
  //       futureResult.map(result =>
  //         result match {
  //           case Some(u) => action(new AuthenticatedRequest(u, request))
  //           case None => Results.Redirect(routes.SessionController.newSession).flashing("error" -> "Please login first.")
  //         }
  //       )
  //     case None => Results.Redirect(routes.SessionController.newSession).flashing("error" -> "Please login first.")
  //   }
  // }

  def index = Action.async { implicit request =>
    // val optionalResult = request.session.get("email").map(repo.getByEmail(_))
    // optionalResult match {
    //   case Some(futureResult) =>
    //     futureResult.map { result =>
    //       result match {
    //         case Some(u) => Ok(views.html.user.index(u))
    //         case None => Redirect(routes.SessionController.newSession).flashing("error" -> "Please login first.")
    //       }
    //     }
    //   case None =>
    //     Future.successful(Redirect(routes.SessionController.newSession).flashing("error" -> "Please login first."))
    // }
    repo.list().map { users =>
      Ok(views.html.user.index(users))
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

  def showUser(id: Long) = Action.async { implicit request =>
    repo.get(id).map { result =>
      result match {
        case Some(u) =>
          Ok(views.html.user.showUser(u))
        case None => Ok(views.html.index())
      }
    }
  }

  def editUser(id: Long) = authenticatedAction.async { implicit request =>
    if (request.user.id != id) {
      println(request.user)
      Future.successful(Redirect(routes.PageController.index()).flashing("error" -> "You're not allowed to do that"))
    } else {
      repo.get(id).map { result =>
        result match {
          case Some(u) =>
            val filledForm = profileForm.fill(ProfileData(u.name, u.email))
            Ok(views.html.user.editUser(id, filledForm))
          case None => Ok(views.html.index())
        }
      }
    }
  }

  def updateUser(id: Long) = authenticatedAction.async { implicit request =>
    profileForm.bindFromRequest.fold(
      errorForm => {
        println(errorForm)
        Future.successful(Ok(views.html.user.editUser(id, errorForm)))
      },
      user => {
        repo.update(id, user.name, user.email).map { result =>
          println(result)
          if (result > 0) {
            Redirect(routes.UserController.showUser(id))
          } else {
            Redirect(routes.UserController.editUser(id))
          }
        }
      }
    )
  }

  def deleteUser(id: Long) = Action.async { implicit requset =>
    repo.delete(id).map { _ =>
      Redirect(routes.UserController.index())
    }
  }
}

