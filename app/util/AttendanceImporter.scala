package utils

import javax.inject._
import play.api.libs.ws._
import play.api.libs.json._
import scala.collection.mutable.ArrayBuffer

import models.AttendanceRepository

case class ClassDetail(groupId: Long, dates: ArrayBuffer[String])

@Singleton
class AttendanceImporter {

  def extractCourseDetail(json: JsValue): IndexedSeq[ClassDetail] = {
    val array = (json \ "classes").as[JsArray].value

    array.map {r =>
      val groupId = (r \ "group_id").as[Long]
      val datesValue = (r \ "dates").as[JsArray].value

      val dates = ArrayBuffer[String]()
      datesValue.foreach { d =>
        dates += d.toString
      }

      ClassDetail(groupId, dates)
    }
  }

  def extractAttendanceDetail(json: JsValue, repo: AttendanceRepository) {
    val attended = (json \ "attended").as[JsArray].value
    val excuse   = (json \ "excuse").as[JsArray].value
    val absent   = (json \ "absent").as[JsArray].value
    val courseId = (json \ "course_id").as[String].toLong
    val groupId = (json \ "group_id").as[String].toInt
    val dateString = (json \ "date").as[String]
    val date     = Utils.convertStringToDate(dateString)

    attended.foreach { id =>
      repo.create(courseId, id.as[String], groupId, date, "attend")
    }

    excuse.foreach { id =>
      repo.create(courseId, id.as[String], groupId, date, "excuse")
    }

    absent.foreach  { id =>
      repo.create(courseId, id.as[String], groupId, date, "absent")
    }
  }
}

