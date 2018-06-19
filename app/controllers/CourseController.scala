package controllers

import javax.inject._
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Failure}

// For Form
import play.api.data._
import play.api.data.format.Formats.doubleFormat
import play.api.data.Forms._
import play.api.data.validation.Constraints._

import java.sql.Date
import java.io.File
import scala.collection.mutable.{Map}

// Model
import models._
import utils._

case class CourseData(title: String, code: String, startDate: Date)
case class FilterData(
  attendanceRate: Int, attendanceRatePoint: Double,
  consecutiveMissed: Int, consecutiveMissedPoint: Double,
  absentCount: Int, absentCountPoint: Double,
  passingMark: Int, overviewThreshold: Double,
  attendanceThreshold: Double, courseworkThreshold: Double,
  courseworkMark: Int, courseworkMarkPoint: Double)
case class CourseworkFilterData(courseworkMark: Int, courseworkMarkPoint: Double, courseworkThreshold: Double)
case class ExamFilterData(passingMark: Int)
case class AttendanceFilterData(
  attendanceRate: Int, attendanceRatePoint: Double,
  consecutiveMissed: Int, consecutiveMissedPoint: Double,
  absentCount: Int, absentCountPoint: Double,
  attendanceThreshold: Double)

class CourseController @Inject()(
  repo: CourseRepository,
  csRepo: CourseStudentRepository,
  aRepo: AttendanceRepository,
  cwRepo: CourseworkRepository,
  eRepo: ExamRepository,
  qRepo: MetricRepository,
  fsRepo: FilterSettingRepository,
  exporter: CourseExporter,
  authenticatedAction: AuthenticatedAction,
  cc: MessagesControllerComponents)
(implicit ec: ExecutionContext) extends
AbstractController(cc) with play.api.i18n.I18nSupport {

  val courseForm = Form {
    mapping(
      "Title" -> nonEmptyText,
      "Code" -> nonEmptyText,
      "Start Date" -> sqlDate
    )(CourseData.apply)(CourseData.unapply)
  }

  val courseworkfilterForm = Form {
    mapping(
      "Coursework Alert Percentage" -> number,
      "Coursework Threshold" -> of(doubleFormat),
      "Coursework Alert Percentage Weightage" -> of(doubleFormat)
    )(CourseworkFilterData.apply)(CourseworkFilterData.unapply)
  }

  val attendanceFilterForm = Form {
    mapping(
      "Attendance Rate" -> number,
      "Attendance Rate Weightage" -> of(doubleFormat),
      "Consecutive Missed Class Count" -> number,
      "Consecutive Missed Class Count Weightage" -> of(doubleFormat),
      "Absent Count" -> number,
      "Absent Count Weightage" -> of(doubleFormat),
      "Attendance Threshold" -> of(doubleFormat),
    )(AttendanceFilterData.apply)(AttendanceFilterData.unapply)
  }

  val examfilterForm = Form {
    mapping(
      "Passing Percentage" -> number,
    )(ExamFilterData.apply)(ExamFilterData.unapply)
  }

  val filterForm = Form {
    mapping(
      "Attendance Rate" -> number,
      "Attendance Rate Weightage" -> of(doubleFormat),
      "Consecutive Missed Class Count" -> number,
      "Consecutive Missed Class Count Weightage" -> of(doubleFormat),
      "Absent Count" -> number,
      "Absent Count Weightage" -> of(doubleFormat),
      "Passing Percentage" -> number,
      "Overview Threshold" -> of(doubleFormat),
      "Attendance Threshold" -> of(doubleFormat),
      "Coursework Threshold" -> of(doubleFormat),
      "Coursework Alert Percentage" -> number,
      "Coursework Alert Percentage Weightage" -> of(doubleFormat)
    )(FilterData.apply)(FilterData.unapply)
  }

  def index = authenticatedAction.async { implicit request =>
    repo.list(request.user.id).map { courses =>
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
    val allStudentFuture  = csRepo.getStudents(id)
    val attendancesFuture = aRepo.getAttendances(id, programme, intake)
    val courseworksFuture = cwRepo.getCourseworks(id, programme, intake)
    val examFuture        = eRepo.getExams(id, programme, intake)
    val filterFuture      = fsRepo.get(id)

    val results = for {
      course      <- courseFuture
      allStudents <- allStudentFuture
      students    <- studentFuture
      attendances <- attendancesFuture
      courseworks <- courseworksFuture
      exam        <- examFuture
      filter      <- filterFuture
    } yield (course, allStudents, students, attendances, courseworks, exam, filter)

    results.map { r =>
      var combined = Utils.combineExamAndCoursework(r._5, r._6)
      var attendance = Utils.combineInsight(r._4, combined)
      var programmeToIntake = Map[String,String]()
      r._2.foreach { s =>
        if (programmeToIntake.contains(s.programme)) {
          val value = programmeToIntake.get(s.programme).get
          programmeToIntake.update(s.programme, value.concat("," + s.intake))
        } else {
          programmeToIntake += (s.programme -> s.intake)
        }
      }
      CourseAPI(r._1, r._3, attendance, combined, r._6, programmeToIntake, r._7.get.overviewThreshold)
    }
  }

  def showCourse(id: Long,
    programme: String = "%",
    intake: String = "%") = Action.async { implicit request =>
    getCourseDetail(id, programme, intake).map { courseApi =>
      courseApi.course match {
        case Some(c) =>
          Ok(views.html.course.showCourse(c, courseApi))
        case None => Redirect(routes.CourseController.index).flashing("error" -> "Course not found.")
      }
    }
  }

  def export(id: Long,
    programme: String = "%",
    intake: String = "%") = authenticatedAction.async { implicit request =>
      getCourseDetail(id, programme, intake).map { courseApi =>
        courseApi.course match {
          case Some(c) =>
            val filename = s"${c.title}.xlsx"
            exporter.export(filename, courseApi)
            Ok.sendFile(
              content = new java.io.File(filename),
              fileName = _ => filename
            )
          case None => Redirect(routes.CourseController.index).flashing("error" -> "Course not found.")
        }
      }
  }

  def createCourse = authenticatedAction.async { implicit request =>
    courseForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(Ok(views.html.course.newCourse(errorForm)))
      },
      course => {
        repo.create(course.title, course.code, course.startDate, request.user.id).map { result =>
          fsRepo.create(result.id, 80, 1, 4, 1, 10, 1, 40, 2, 2, 2, 40, 1)
          Redirect(routes.CourseController.importStudent(result.id)).flashing("success" -> "Course has been successfully created.")
        }
      }
    )
  }

  def importStudent(id: Long) = authenticatedAction { implicit request =>
    Ok(views.html.course.importStudent(id))
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
          .toRight(Redirect(routes.CourseController.index).flashing("error" -> "You're not allowed to do that"))
      )
    }
  }

  def PermissionCheckAction(implicit ec: ExecutionContext) = new ActionFilter[CourseRequest] {
    def executionContext = ec
    def filter[A](input: CourseRequest[A]) = Future.successful {
        if (input.user.id != input.course.userId) {
          Some(Redirect(routes.CourseController.index).flashing("error" -> "You're not allowed to do that"))
        } else { None }
    }
  }

  def editCourse(id: Long) = (authenticatedAction andThen CourseAction(id) andThen PermissionCheckAction) { implicit request =>
    val filledForm = courseForm.fill(CourseData(request.course.title, request.course.code, request.course.startDate))
    Ok(views.html.course.editCourse(id, filledForm))
  }

  def filterSetting(id: Long) = (authenticatedAction andThen CourseAction(id) andThen PermissionCheckAction).async { implicit request =>
    fsRepo.get(id).map { option =>
      option match {
        case Some(s) =>
          Ok(views.html.course.filterSetting(id, request.course, s))
        case None => Redirect(routes.CourseController.index()).flashing("error" -> "Setting not found.")
      }
    }
  }

  def editCWSetting(id: Long) = (authenticatedAction andThen CourseAction(id) andThen PermissionCheckAction).async { implicit request =>
    fsRepo.get(id).map { option =>
      option match {
        case Some(s) =>
        val filledForm = courseworkfilterForm.fill(CourseworkFilterData(
          s.courseworkMark, s.courseworkThreshold, s.courseworkMarkPoint
        ))
          Ok(views.html.course.editCWSetting(id, filledForm))
        case None => Redirect(routes.CourseController.index).flashing("error" -> "Setting not found.")
      }
    }
  }

  def updateCWSetting(id: Long) = (authenticatedAction andThen CourseAction(id) andThen PermissionCheckAction).async { implicit request =>
    courseworkfilterForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(Ok(views.html.course.editCWSetting(id, errorForm)))
      },
      s => {
        fsRepo.updateCW(id,
          s.courseworkThreshold, s.courseworkMark, s.courseworkMarkPoint).map { result =>
          Redirect(routes.CourseworkController.index(id)).flashing("success" -> "Coursework rules has been successfully updated.")
        }
      }
    )
  }

  def editESetting(id: Long) = (authenticatedAction andThen CourseAction(id) andThen PermissionCheckAction).async { implicit request =>
    fsRepo.get(id).map { option =>
      option match {
        case Some(s) =>
        val filledForm = examfilterForm.fill(ExamFilterData(
          s.passingMark))
          Ok(views.html.course.editESetting(id, filledForm))
        case None => Redirect(routes.CourseController.index()).flashing("error" -> "Setting not found.")
      }
    }
  }

  def updateESetting(id: Long) = (authenticatedAction andThen CourseAction(id) andThen PermissionCheckAction).async { implicit request =>
    examfilterForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(Ok(views.html.course.editESetting(id, errorForm)))
      },
      s => {
        fsRepo.updateE(id, s.passingMark).map { result =>
          Redirect(routes.ExaminationController.index(id)).flashing("success" -> "Exam Rules has been successfully updated.")
        }
      }
    )
  }

  def editASetting(id: Long) = (authenticatedAction andThen CourseAction(id) andThen PermissionCheckAction).async { implicit request =>
    fsRepo.get(id).map { option =>
      option match {
        case Some(s) =>
        val filledForm = attendanceFilterForm.fill(AttendanceFilterData(
          s.attendanceRate, s.attendanceRatePoint,
          s.consecutiveMissed, s.consecutiveMissedPoint,
          s.absentCount, s.absentCountPoint,
          s.attendanceThreshold))
          Ok(views.html.course.editASetting(id, filledForm))
        case None => Redirect(routes.CourseController.index()).flashing("error" -> "Setting not found.")
      }
    }
  }

  def updateASetting(id: Long) = (authenticatedAction andThen CourseAction(id) andThen PermissionCheckAction).async { implicit request =>
    attendanceFilterForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(Ok(views.html.course.editASetting(id, errorForm)))
      },
      s => {
        fsRepo.updateA(id,
          s.attendanceRate, s.attendanceRatePoint,
          s.consecutiveMissed, s.consecutiveMissedPoint,
          s.absentCount, s.absentCountPoint,
          s.attendanceThreshold).map { result =>
          Redirect(routes.AttendanceController.index(id)).flashing("success" -> "Rules has been successfully updated.")
        }
      }
    )
  }

  def editSetting(id: Long) = (authenticatedAction andThen CourseAction(id) andThen PermissionCheckAction).async { implicit request =>

    fsRepo.get(id).map { option =>
      option match {
        case Some(s) =>
        val filledForm = filterForm.fill(FilterData(
          s.attendanceRate, s.attendanceRatePoint,
          s.consecutiveMissed, s.consecutiveMissedPoint,
          s.absentCount, s.absentCountPoint,
          s.passingMark, s.overviewThreshold,
          s.attendanceThreshold, s.courseworkThreshold,
          s.courseworkMark, s.courseworkMarkPoint
        ))
          Ok(views.html.course.editSetting(id, filledForm))
        case None => Redirect(routes.CourseController.index()).flashing("error" -> "Setting not found.")
      }
    }
  }

  def updateSetting(id: Long) = (authenticatedAction andThen CourseAction(id) andThen PermissionCheckAction).async { implicit request =>
    filterForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(Ok(views.html.course.editSetting(id, errorForm)))
      },
      s => {
        fsRepo.update(id,
          s.attendanceRate, s.attendanceRatePoint,
          s.consecutiveMissed, s.consecutiveMissedPoint,
          s.absentCount, s.absentCountPoint,
          s.passingMark, s.overviewThreshold,
          s.attendanceThreshold, s.courseworkThreshold,
          s.courseworkMark, s.courseworkMarkPoint).map { result =>
          Redirect(routes.CourseController.index).flashing("success" -> "Rules has been successfully updated.")
        }
      }
    )
  }

  def updateCourse(id: Long) = (authenticatedAction andThen CourseAction(id) andThen PermissionCheckAction).async { implicit request =>
    courseForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(Ok(views.html.course.editCourse(id, errorForm)))
      },
      course => {
        repo.update(id, course.title, course.code, course.startDate).map { result =>
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

  def importation(id: Long) = (authenticatedAction andThen CourseAction(id) andThen PermissionCheckAction) { implicit request =>
    Ok(views.html.course.importation(request.course))
  }

}
