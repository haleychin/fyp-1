package controllers

import javax.inject._
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Failure}

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

class UploadController @Inject()(
  repo: CourseStudentRepository,
  authenticatedAction: AuthenticatedAction,
  cc: MessagesControllerComponents)
(implicit ec: ExecutionContext) extends
AbstractController(cc) with play.api.i18n.I18nSupport {

  def upload = authenticatedAction(parse.multipartFormData) { implicit request =>
    request.body.file("file").map { file =>
      val filename = Paths.get(file.filename).getFileName
      file.ref.moveTo(Paths.get(s"$filename"), replace = true)
      println(file)

      import scala.collection.JavaConversions._
      val workbook = WorkbookFactory.create(new File(filename.toString()))
        val formatter = new DataFormatter()
        for {
            // Iterate and print the sheets
            (sheet, i) <- workbook.zipWithIndex
            _ = println(s"Sheet $i of ${workbook.getNumberOfSheets}: ${sheet.getSheetName}")

            // Iterate and print the rows
            row <- sheet
            _ = println(s"\tRow ${row.getRowNum}")

            // Iterate and print the cells
            cell <- row
        } {
            println(s"\t\t${cell.getAddress}: ${formatter.formatCellValue(cell)}")
        }
        // Redirect(routes.PageController.index).flashing(
        //   "success" -> "File uploaded successfully")
        Ok("File uploaded")
    }.getOrElse {
      Redirect(routes.PageController.index).flashing(
        "error" -> "Missing file")
    }
  }

}
