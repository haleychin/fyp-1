package controllers

import javax.inject._
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

// For Form
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

// For Files
import java.nio.file.Paths
import java.io.File

// Model
import models._
import utils._

case class CourseworkFormData(courseworks: List[String])

class CourseworkController @Inject()(
  repo: CourseworkRepository,
  bbParser: BlackboardParser,
  authenticatedAction: AuthenticatedAction,
  cc: MessagesControllerComponents)
(implicit ec: ExecutionContext) extends
AbstractController(cc) with play.api.i18n.I18nSupport {

  val form = Form {
    mapping(
      "courseworks" -> list(text)
    )(CourseworkFormData.apply)(CourseworkFormData.unapply)
  }

  def newImport(courseId: Long) = authenticatedAction { implicit request =>
    Ok(views.html.coursework.newImport(courseId))
  }

  def save(courseId: Long) = authenticatedAction(parse.multipartFormData) { implicit request =>
    request.body.file("file").map { file =>
      val filename = Paths.get(file.filename).getFileName
      file.ref.moveTo(Paths.get(s"$filename"), replace = true)

      // Start Parsing
      bbParser.parse(filename.toString())
      println(bbParser.lines)

      Redirect(routes.CourseworkController.selection(courseId)).flashing(
        "success" -> s"Import courseworks successfully")
    }.getOrElse {
      Redirect(routes.PageController.index).flashing(
        "error" -> "Missing file")
    }
  }

  def selection(courseId: Long) = authenticatedAction { implicit request =>
    val courseworks = bbParser.header.map { case (value, _) =>
      (value, value)
    }.toSeq
    Ok(views.html.coursework.selection(courseId, form, courseworks))
  }

  def saveSelection(courseId: Long) = authenticatedAction { implicit request =>
    form.bindFromRequest.fold(
      errorForm => {
        val courseworks = bbParser.header.map { case (value, _) =>
          (value, value)
        }.toSeq
        Ok(views.html.coursework.selection(courseId, errorForm, courseworks))
      },
      form => {
        bbParser.getCourseworks(form.courseworks)
        bbParser.saveToDb(courseId, repo)
        Ok(views.html.index())
      }
    )
  }

  def blackboard = Action { implicit request =>
    bbParser.parse( "BlackboardExport.txt")
    Ok(views.html.index())
  }

  def finalExam = Action { implicit request =>
    FinalExamParser.parse("FinalExam.xlsx")
    Ok(views.html.index())
  }

}

