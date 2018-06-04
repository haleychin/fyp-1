package controllers

import javax.inject._
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Failure}

// For Form
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

import java.sql.Date

// Model
import models._
import utils._

case class CourseData(title: String, startDate: Date)
case class CourseAPI(
  course: Option[Course],
  students: Seq[Student],
  attendance: AttendanceAPI,
  coursework: CourseworkAPI)

class CourseController @Inject()(
  repo: CourseRepository,
  csRepo: CourseStudentRepository,
  aRepo: AttendanceRepository,
  cwRepo: CourseworkRepository,
  eRepo: ExamRepository,
  authenticatedAction: AuthenticatedAction,
  cc: MessagesControllerComponents)
(implicit ec: ExecutionContext) extends
AbstractController(cc) with play.api.i18n.I18nSupport {

  val courseForm = Form {
    mapping(
      "title" -> nonEmptyText,
      "startDate" -> sqlDate
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

  def getCourseDetail(id: Long,
    programme: String,
    intake: String): Future[CourseAPI] = {
    val courseFuture      = repo.get(id)
    val studentFuture     = csRepo.getStudents(id, programme, intake)
    val attendancesFuture = aRepo.getAttendances(id, programme, intake)
    val courseworksFuture = cwRepo.getCourseworks(id, programme, intake)
    val examFuture        = eRepo.getExams(id, programme, intake)

    val results = for {
      course      <- courseFuture
      students    <- studentFuture
      attendances <- attendancesFuture
      courseworks <- courseworksFuture
      exam        <- examFuture
    } yield (course, students, attendances, courseworks, exam)

    results.map { r =>
      var combined = Utils.combineExamAndCoursework(r._4, r._5)
      combined = Utils.combineInsight(r._3, combined)
      CourseAPI(r._1, r._2, r._3, combined)
    }
  }

  def showCourse(id: Long,
    programme: String = "%",
    intake: String = "%") = Action.async { implicit request =>
    getCourseDetail(id, programme, intake).map { courseApi =>
      courseApi.course match {
        case Some(c) =>
          Ok(views.html.course.showCourse(c, courseApi))
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
        repo.create(course.title, course.startDate, request.user.id).map { result =>
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
    val filledForm = courseForm.fill(
      CourseData(request.course.title, request.course.startDate))
    Ok(views.html.course.editCourse(id, filledForm))
  }

  def updateCourse(id: Long) = (authenticatedAction andThen CourseAction(id) andThen PermissionCheckAction).async { implicit request =>
    courseForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(Ok(views.html.course.editCourse(id, errorForm)))
      },
      course => {
        repo.update(id, course.title, course.startDate).map { result =>
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
