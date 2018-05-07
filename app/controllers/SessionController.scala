package controllers

import javax.inject._
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}

// For Form
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

// Model
import models._

// BCrypt
import org.mindrot.jbcrypt.BCrypt

case class LoginData(email: String, password: String)

class SessionController @Inject()(repo: UserRepository,
  cc: MessagesControllerComponents)
(implicit ec: ExecutionContext) extends
MessagesAbstractController(cc) {

  // Define Login Form
  val loginForm = Form {
    mapping(
      "email" -> email,
      "password" -> nonEmptyText(minLength = 6)
    )(LoginData.apply)(LoginData.unapply)
  }

  def newSession = Action { implicit request =>
    Ok(views.html.session.newSession(loginForm))
  }

  def createSession = Action.async { implicit request =>
    loginForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(Ok(views.html.session.newSession(errorForm)))
      },
      user => {
        // Find User
        // Checkpw with BCrypt.checkpw(user.password, foundUser.password)
        Future.successful(Redirect(routes.UserController.index).flashing("success" -> "Successfully login"))
      }
    )
  }



}

