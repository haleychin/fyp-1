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

  def newCourse = authenticatedAction { implicit request =>
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

  def createCourse = authenticatedAction.async { implicit request =>
    courseForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(Ok(views.html.course.newCourse(errorForm)))
      },
      course => {
        repo.create(course.title, request.user.id).map { result =>
          Redirect(routes.CourseController.index).flashing("success" -> "Course has been successfully created.")
        }
      }
    )
  }

  class CourseRequest[A](val course: Course , request: AuthenticatedRequest[A]) extends WrappedRequest[A](request) {
    def user = request.user
  }

  def CourseAction(courseId: Long)(implicit ec: ExecutionContext) = new ActionRefiner[AuthenticatedRequest, CourseRequest] {
    def executionContext = ec
    def refine[A](input: AuthenticatedRequest[A]) =  {
      repo.get(courseId).map(result =>
        result
          .map(c => new CourseRequest(c, input))
          .toRight(Redirect(routes.PageController.index()).flashing("error" -> "You're not allowed to do that"))
      )
    }
  }

  def PermissionCheckAction(implicit ec: ExecutionContext) = new ActionFilter[CourseRequest] {
    def executionContext = ec
    def filter[A](input: CourseRequest[A]) = Future.successful {
        if (input.user.id != input.course.userId) {
          Some(Redirect(routes.PageController.index()).flashing("error" -> "You're not allowed to do that"))
        } else { None }
    }
  }

  def editCourse(id: Long) = (authenticatedAction andThen CourseAction(id) andThen PermissionCheckAction) { implicit request =>
    val filledForm = courseForm.fill(CourseData(request.course.title))
    Ok(views.html.course.editCourse(id, filledForm))
  }

  def updateCourse(id: Long) = (authenticatedAction andThen CourseAction(id) andThen PermissionCheckAction).async { implicit request =>
    courseForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(Ok(views.html.course.editCourse(id, errorForm)))
      },
      course => {
        repo.update(id, course.title).map { result =>
          Redirect(routes.CourseController.index).flashing("success" -> "Course has been successfully updated.")
        }
      }
    )
  }

  def deleteCourse(id: Long) = (authenticatedAction andThen CourseAction(id) andThen PermissionCheckAction).async { implicit request =>
    repo.delete(id).map { _ =>
      Redirect(routes.CourseController.index).flashing("success" ->
        "Course has been successfully deleted.")
    }
  }

}
