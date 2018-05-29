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

case class CourseAPI(course: Option[Course], attendance: AttendanceAPI)

class AttendanceController @Inject()(
  ws: WSClient,
  aImporter: AttendanceImporter,
  repo: AttendanceRepository,
  cRepo: CourseRepository,
  authenticatedAction: AuthenticatedAction,
  cc: MessagesControllerComponents)
(implicit ec: ExecutionContext) extends
AbstractController(cc) with play.api.i18n.I18nSupport {

  def wsRequest(url: String): WSRequest = {
    ws.url(url)
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)
  }

  def getCourseDetail(id: Long): Future[CourseAPI] = {
    val courseFuture = cRepo.get(id)
    val attendancesFuture = repo.getAttendances(id)

    val results = for {
      course <- courseFuture
      attendances <- attendancesFuture
    } yield (course, attendances)


    results.map { r =>
      println(r._2)
      CourseAPI(r._1, r._2)
    }
  }

  def index(id: Long) = Action.async { implicit request =>
    getCourseDetail(id).map { courseApi =>
      courseApi.course match {
        case Some(c) =>
          Ok(views.html.attendance.index(c, courseApi.attendance))
        case None => Ok(views.html.index())
      }
    }
  }

  def newAttendance(id: Long) = authenticatedAction.async { implicit request =>
    wsRequest(s"http://localhost:4567/courses/$id")
      .get()
      .map { r =>
        val data = aImporter.extractCourseDetail(r.json)
        Ok(views.html.attendance.newAttendance(id, data))
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
        Ok(views.html.index())
      }
  }

}

