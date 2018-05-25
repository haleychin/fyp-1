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

class CourseworkController @Inject()(
  repo: CourseworkRepository,
  authenticatedAction: AuthenticatedAction,
  cc: MessagesControllerComponents)
(implicit ec: ExecutionContext) extends
AbstractController(cc) with play.api.i18n.I18nSupport {

  def newImport(courseId: Long) = authenticatedAction { implicit request =>
    Ok(views.html.coursework.newImport(courseId))
  }

  def save(courseId: Long) = authenticatedAction(parse.multipartFormData) { implicit request =>
    request.body.file("file").map { file =>
      val filename = Paths.get(file.filename).getFileName
      file.ref.moveTo(Paths.get(s"$filename"), replace = true)

      Redirect(routes.PageController.index).flashing(
        "success" -> s"Import courseworks successfully")
    }.getOrElse {
      Redirect(routes.PageController.index).flashing(
        "error" -> "Missing file")
    }
  }

  def blackboard = Action { implicit request =>
    BlackboardParser.parse(
      "BlackboardExport.txt",
      Array(
        "Project Part 1 [Total Pts: 10 Score] |17955",
        "Project Part 2 [Total Pts: 30 Score] |18144",
        "MidTerm [Total Pts: 20 Score] |18278"
      )
    )
    Ok(views.html.index())
  }

  def finalExam = Action { implicit request =>
    FinalExamParser.parse("FinalExam.xlsx")
    Ok(views.html.index())
  }

}

