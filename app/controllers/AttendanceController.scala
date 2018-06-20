package controllers

import javax.inject._
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import play.api.libs.ws._
import play.api.libs.json._

// Model
import models._
import utils._

case class CourseAttendanceAPI(
  course: Option[Course],
  students: Seq[Student],
  attendance: AttendanceAPI,
  unenrolled: Iterable[Student],
  threshold: Double)

class AttendanceController @Inject()(
  ws: WSClient,
  aImporter: AttendanceImporter,
  repo: AttendanceRepository,
  cRepo: CourseRepository,
  csRepo: CourseStudentRepository,
  fsRepo: FilterSettingRepository,
  authenticatedAction: AuthenticatedAction,
  cc: MessagesControllerComponents)
(implicit ec: ExecutionContext) extends
AbstractController(cc) with play.api.i18n.I18nSupport {

  def wsRequest(url: String): WSRequest = {
    ws.url(url)
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)
  }

  def getCourseDetail(id: Long): Future[CourseAttendanceAPI] = {
    val courseFuture = cRepo.get(id)
    val studentFuture = csRepo.getStudents(id)
    val attendancesFuture = repo.getAttendances(id)
    val filterFuture = fsRepo.get(id)

    val results = for {
      course <- courseFuture
      students <- studentFuture
      attendances <- attendancesFuture
      filter <- filterFuture
    } yield (course, students, attendances, filter)


    results.map { r =>
      val icheckInStudents = r._3.studentDetails.values.map(_.student)
      val unenrolled = r._2.diff(icheckInStudents.toSeq)
      println(r._3)
      CourseAttendanceAPI(r._1, r._2, r._3, unenrolled, r._4.get.attendanceThreshold)
    }
  }

  def index(id: Long) = Action.async { implicit request =>
    getCourseDetail(id).map { courseApi =>
      courseApi.course match {
        case Some(c) =>
          Ok(views.html.attendance.index(c,
            courseApi.students,
            courseApi.attendance,
            courseApi.unenrolled,
            courseApi.threshold))
        case None =>
          Redirect(routes.CourseController.index).flashing("error" -> "Course not found.")
      }
    }
  }

  def newAttendance(id: Long) = authenticatedAction.async { implicit request =>
    wsRequest(s"http://localhost:4567/courses/$id")
      .get()
      .map { r =>
        val data = aImporter.extractCourseDetail(r.json)
        data.foreach { cd =>
          cd.dates.foreach { date =>
            wsRequest("http://localhost:4567/attendance")
              .addQueryStringParameters(
                "course_id" -> id.toString,
                "date" -> date,
                "group_id" -> cd.groupId.toString)
              .get()
              .map { r =>
                aImporter.extractAttendanceDetail(r.json, repo)
              }
          }
        }
        Ok(views.html.attendance.newAttendance(id, data))
      }
  }

  def delete(id: Long) = authenticatedAction.async { implicit request =>
    repo.delete(id).map { _ =>
      Redirect(routes.AttendanceController.index(id)).flashing(
        "success" -> "Successfully remove all attendance records.")
    }
  }

  def fetch(id: Long, groupId: Int, date: String) = authenticatedAction.async { implicit request =>
    wsRequest("http://localhost:4567/attendance")
      .addQueryStringParameters(
        "course_id" -> id.toString,
        "date" -> date,
        "group_id" -> groupId.toString)
      .get()
      .map { r =>
        aImporter.extractAttendanceDetail(r.json, repo)
        Redirect(routes.AttendanceController.index(id)).flashing("success" -> "Successfully fetch attendances from iCheckin.")
      }
  }

}

