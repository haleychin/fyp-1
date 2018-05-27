package controllers

import javax.inject._
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

// For Files
import java.nio.file.Paths
import java.io.File

// Model
import models._
import utils._

class ExamController @Inject()(
  repo: ExamRepository,
  eParser: FinalExamParser,
  authenticatedAction: AuthenticatedAction,
  cc: MessagesControllerComponents)
(implicit ec: ExecutionContext) extends
AbstractController(cc) with play.api.i18n.I18nSupport {

  def newImport(courseId: Long) = authenticatedAction { implicit request =>
    Ok(views.html.exam.newImport(courseId))
  }

  def save(courseId: Long) = authenticatedAction(parse.multipartFormData) { implicit request =>
    request.body.file("file").map { file =>
      val filename = Paths.get(file.filename).getFileName
      file.ref.moveTo(Paths.get(s"$filename"), replace = true)

      eParser.save(filename.toString(), courseId, repo)
      Redirect(routes.ExamController.selection(courseId)).flashing(
        "success" -> s"Import final exam results successfully")
    }.getOrElse {
      Redirect(routes.PageController.index).flashing(
        "error" -> "Missing file")
    }
  }

  def selection(courseId: Long) = authenticatedAction { implicit request =>
    Ok(views.html.index())
  }
}

