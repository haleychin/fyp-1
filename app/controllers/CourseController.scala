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

case class CourseData(title: String)

class CourseController @Inject()(
  repo: CourseRepository,
  authenticatedAction: AuthenticatedAction,
  cc: MessagesControllerComponents)
(implicit ec: ExecutionContext) extends
AbstractController(cc) with play.api.i18n.I18nSupport {

  val courseForm = Form {
    mapping(
      "title" -> nonEmptyText
    )(CourseData.apply)(CourseData.unapply)
  }

  def index = Action.async { implicit request =>
    repo.list().map { courses =>
      Ok(views.html.course.index(courses))
    }
  }

  def newCourse = Action { implicit request =>
    Ok(views.html.course.newCourse(courseForm))
  }

  def showCourse(id: Long) = Action.async { implicit request =>
    repo.get(id).map { result =>
      result match {
        case Some(c) =>
          Ok(views.html.course.showCourse(c))
        case None => Ok(views.html.index())
      }
    }
  }

  def createCourse = Action { implicit request =>
    Ok(views.html.index())
  }

  def editCourse(id: Long) = Action { implicit request =>
    Ok(views.html.index())
  }

  def updateCourse(id: Long) = Action { implicit request =>
    Ok(views.html.index())
  }

  def deleteCourse(id: Long) = Action { implicit request =>
    Ok(views.html.index())
  }

}
