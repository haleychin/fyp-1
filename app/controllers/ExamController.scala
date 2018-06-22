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

case class CourseExamAPI(
  course: Option[Course],
  students: Seq[Student],
  exams: ExamAPI)

class ExamController @Inject()(
  repo: ExamRepository,
  csRepo: CourseStudentRepository,
  cRepo: CourseRepository,
  qRepo: QuestionRepository,
  mRepo: MetricRepository,
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
      if (s"$filename" != "") {
        file.ref.moveTo(Paths.get(s"$filename"), replace = true)

        try {
          eParser.saveWithMetric(filename.toString(), courseId, repo,
            qRepo, mRepo)
          Redirect(routes.ExaminationController.index(courseId)).flashing(
            "success" -> s"Import final exam results successfully")
        } catch {
          case e: Exception =>
            Redirect(routes.CourseController.importation(courseId)).flashing("error" -> "Incorrect Excel format provided.")
        }
      } else {
        Redirect(routes.CourseController.showCourse(courseId)).flashing(
          "error" -> "Missing exam excel files")
      }
    }.getOrElse {
      Redirect(routes.CourseController.showCourse(courseId)).flashing(
        "error" -> "Missing exam excel files")
    }
  }
}

