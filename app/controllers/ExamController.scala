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

case class CourseExamAPI(course: Option[Course], exams: ExamAPI)

class ExamController @Inject()(
  repo: ExamRepository,
  cRepo: CourseRepository,
  eParser: FinalExamParser,
  authenticatedAction: AuthenticatedAction,
  cc: MessagesControllerComponents)
(implicit ec: ExecutionContext) extends
AbstractController(cc) with play.api.i18n.I18nSupport {

  def getExamDetails(courseId: Long): Future[CourseExamAPI] = {
    val courseFuture = cRepo.get(courseId)
    val examsFuture = repo.getExams(courseId)

    val results = for {
      course <- courseFuture
      exams <- examsFuture
    } yield (course, exams)


    results.map { r =>
      CourseExamAPI(r._1, r._2)
    }
  }

  def index(courseId: Long) = Action.async { implicit request =>
    getExamDetails(courseId).map { examApi =>
      examApi.course match {
        case Some(c) =>
          Ok(views.html.exam.index(c, examApi.exams))
        case None => Ok(views.html.index())
      }
    }
  }

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

