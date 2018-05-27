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
        println(data)
        val dates = Array("2018", "2017")
        Ok(views.html.attendance.newAttendance(id, dates))
      }
  }

  def index = Action { implicit request =>
    wsRequest("http://localhost:4567/courses/3")
      .get()
      .map { r =>
        // extractCourseDetail(r.json)
      }

    wsRequest("http://localhost:4567/attendance")
      .addQueryStringParameters(
        "course_id" -> "3",
        "date" -> "19/07/2017",
        "group_id" -> "2")
      .get()
      .map { r =>
        // extractAttendanceDetail(r.json)
        println(r.json)
      }

    Ok(views.html.index())
  }
}

