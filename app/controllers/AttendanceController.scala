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

class AttendanceController @Inject()(
  ws: WSClient,
  aImporter: AttendanceImporter,
  repo: AttendanceRepository,
  authenticatedAction: AuthenticatedAction,
  cc: MessagesControllerComponents)
(implicit ec: ExecutionContext) extends
AbstractController(cc) with play.api.i18n.I18nSupport {

  def wsRequest(url: String): WSRequest = {
    ws.url(url)
      .addHttpHeaders("Accept" -> "application/json")
      .withRequestTimeout(10000.millis)
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

