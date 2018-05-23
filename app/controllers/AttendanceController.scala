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

  def extractCourseDetail(json: JsValue) {
    val array = (json \ "classes").as[JsArray].value

    val map = array.map {r =>
      val group_id = (r \ "group_id").as[Long]
      val dates = (r \ "dates").as[JsArray].value
    }
  }

  def extractAttendanceDetail(json: JsValue) {
    val attended = (json \ "attended").as[JsArray].value
    val excuse   = (json \ "excuse").as[JsArray].value
    val absent   = (json \ "absent").as[JsArray].value
    val courseId = (json \ "course_id").as[String].toLong
    val dateString = (json \ "date").as[String]
    val date     = Utils.convertStringToDate(dateString)

    attended.foreach { id =>
      repo.create(courseId, id.as[String], date, "attend").map(println(_))
    }

    excuse.foreach { id =>
      repo.create(courseId, id.as[String], date, "excuse").map(println(_))
    }

    println(absent)
    absent.foreach  { id =>
      repo.create(courseId, id.as[String], date, "absent").map(println(_))
    }
  }

  def index = Action { implicit request =>
    wsRequest("http://localhost:4567/courses/1")
      .get()
      .map { r =>
        extractCourseDetail(r.json)
      }

    wsRequest("http://localhost:4567/attendance")
      .addQueryStringParameters(
        "course_id" -> "1",
        "date" -> "19/07/2017",
        "group_id" -> "2")
      .get()
      .map { r =>
        extractAttendanceDetail(r.json)
      }

    Ok(views.html.index())
  }

  def test = Action { implicit request =>
    BlackboardParser.parse("BlackboardExport.txt")
    Ok(views.html.index())
  }

}

