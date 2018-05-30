package controllers

import javax.inject._
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Failure}
import org.postgresql.util.PSQLException

// For Form
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

// For Files
import java.nio.file.Paths
import java.io.File
import org.apache.poi.ss.usermodel.{WorkbookFactory, DataFormatter}

// Model
import models._
import utils._

class UploadController @Inject()(
  repo: CourseStudentRepository,
  sRepo: StudentRepository,
  authenticatedAction: AuthenticatedAction,
  cc: MessagesControllerComponents)
(implicit ec: ExecutionContext) extends
AbstractController(cc) with play.api.i18n.I18nSupport {

  def upload(courseId: Long) = authenticatedAction(parse.multipartFormData) { implicit request =>
    request.body.file("file").map { file =>
      val filename = Paths.get(file.filename).getFileName
      file.ref.moveTo(Paths.get(s"$filename"), replace = true)

      import scala.collection.JavaConversions._
      import scala.collection.mutable.ArrayBuffer

      val workbook = WorkbookFactory.create(new File(filename.toString()))
      val formatter = new DataFormatter()
      val sheet = workbook.getSheetAt(0)
      val errorMessages = ArrayBuffer[String]()
      var successCount = 0

      for (row <- sheet) {
        if (row.getRowNum() == 0) {}
        else {
          val values = row.map(formatter.formatCellValue(_)).toArray
          val date = row.getCell(6).getDateCellValue()
          val dateString = Utils.getDateAsString(date)

          sRepo.create(values(0), values(1), values(2), values(3),
            values(4), values(5), Utils.convertStringToDate(dateString),
            values(7), values(8), values(9).toInt).map { r =>
              r match {
                case Success(u) =>
                  successCount += 1
                  repo.create(courseId, u.id)
                case Failure(e: PSQLException) =>
                  repo.createWithStudentId(courseId, values(2))
                  errorMessages += e.getServerErrorMessage().getDetail()
              }
            }
        }
      }

      Redirect(routes.PageController.index).flashing(
        "success" -> s"Import $successCount students successfully",
        "error" -> s"${errorMessages.mkString(" ")}")
    }.getOrElse {
      Redirect(routes.PageController.index).flashing(
        "error" -> "Missing file")
    }

  }

}
