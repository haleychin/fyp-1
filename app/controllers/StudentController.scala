
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

case class StudentData(title: String)

class StudentController @Inject()(
  // repo: StudentRepository,
  authenticatedAction: AuthenticatedAction,
  cc: MessagesControllerComponents)
(implicit ec: ExecutionContext) extends
AbstractController(cc) with play.api.i18n.I18nSupport {

  val studentForm = Form {
    mapping(
      "title" -> nonEmptyText
    )(StudentData.apply)(StudentData.unapply)
  }

  def index = Action { implicit request =>
    Ok(views.html.index())
  }

  def newStudent = Action { implicit request =>
    Ok(views.html.index())
  }

  def showStudent(id: Long) = Action { implicit request =>
    Ok(views.html.index())
  }

  def createStudent = Action { implicit request =>
    Ok(views.html.index())
  }

  def editStudent(id: Long) = Action { implicit request =>
    Ok(views.html.index())
  }

  def updateStudent(id: Long) = Action { implicit request =>
    Ok(views.html.index())
  }

  def deleteStudent(id: Long) = Action { implicit request =>
    Ok(views.html.index())
  }
}
