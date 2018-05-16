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

case class CourseStudentData(studentId: Long)

class CourseStudentController @Inject()(
  repo: CourseStudentRepository,
  authenticatedAction: AuthenticatedAction,
  cc: MessagesControllerComponents)
(implicit ec: ExecutionContext) extends
AbstractController(cc) with play.api.i18n.I18nSupport {

  val form = Form {
    mapping(
      "studentId" -> longNumber
    )(CourseStudentData.apply)(CourseStudentData.unapply)
  }

  def newCourseStudent(courseId: Long) = authenticatedAction { implicit request =>
    Ok(views.html.courseStudent.newCourseStudent(courseId, form))
  }

  def create(courseId: Long) = authenticatedAction.async { implicit request =>
    form.bindFromRequest.fold(
      errorForm => {
        Future.successful(Ok(views.html.courseStudent.newCourseStudent(courseId, errorForm)))
      },
      courseStudent => {
        repo.create(courseId, courseStudent.studentId).map { result =>
          Redirect(routes.CourseController.index).flashing("success" -> "Student has been added to course.")
        }
      }
    )
  }

  def delete(courseId: Long, studentId: Long) = authenticatedAction.async { implicit request =>
    repo.delete(courseId, studentId).map { _ =>
      Redirect(routes.CourseController.index).flashing("success" ->
        "Student has been removed from course.")
    }
  }

}
