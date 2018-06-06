package utils

import javax.inject._
import play.api.libs.ws._
import play.api.libs.json._
import scala.collection.mutable.ArrayBuffer
import java.util.Date
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Failure}
import org.postgresql.util.PSQLException

import models.{CourseStudentRepository,StudentRepository}

case class ImportStatus(count: Int, messages: ArrayBuffer[String])
@Singleton
class StudentImporter {

  def importStudent(
    json: JsValue,
    repo: StudentRepository,
    csRepo: CourseStudentRepository)(implicit ec: ExecutionContext) {
    val courseId = (json \ "id").as[String].toLong
    val students = (json \ "students").as[JsArray].value
    var successCount = 0
    var errorMessages = ArrayBuffer[String]()

    students.map { values =>
      val date = values(6).as[String]
      repo.create(
        values(0).as[String],
        values(1).as[String],
        values(2).as[Long].toString,
        values(3).as[String],
        values(4).as[String],
        values(5).as[String],
        Utils.convertStringToDate(date),
        values(7).as[String],
        values(8).as[String],
        values(9).as[Int]).map { r =>
          r match {
            case Success(u) =>
              successCount += 1
              csRepo.create(courseId, u.id)
            case Failure(e: PSQLException) =>
              csRepo.createWithStudentId(courseId, values(2).as[String])
              errorMessages += e.getServerErrorMessage().getDetail()
          }
        }
    }
  }
}

