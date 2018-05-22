package controllers

import javax.inject._
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import play.api.libs.ws._
import play.api.libs.json._


// Model
import models._

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
    var attended = (json \ "attended").as[JsArray].value
    var excuse   = (json \ "excuse").as[JsArray].value
    var absent   = (json \ "absent").as[JsArray].value

    println(attended)
    println(excuse)
    println(absent)
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
        "date" -> "2017-07-19",
        "group_id" -> "2")
      .get()
      .map { r =>
        println(r)
        extractAttendanceDetail(r.json)
      }

    Ok(views.html.index())
  }

}

