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

class ExaminationController @Inject()(
  repo: ExamRepository,
  csRepo: CourseStudentRepository,
  cRepo: CourseRepository,
  qRepo: QuestionRepository,
  mRepo: MetricRepository,
  authenticatedAction: AuthenticatedAction,
  cc: MessagesControllerComponents)
(implicit ec: ExecutionContext) extends
AbstractController(cc) with play.api.i18n.I18nSupport {

  def getExamDetails(courseId: Long): Future[CourseExamAPI] = {
    val courseFuture = cRepo.get(courseId)
    val studentFuture = csRepo.getStudents(courseId)
    val examsFuture = repo.getExams(courseId)

    mRepo.getMetrics(courseId)

    val results = for {
      course <- courseFuture
      students <- studentFuture
      exams <- examsFuture
    } yield (course, students, exams)


    results.map { r =>
      CourseExamAPI(r._1, r._2, r._3)
    }
  }

  def index(courseId: Long) = Action.async { implicit request =>
    getExamDetails(courseId).map { examApi =>
      examApi.course match {
        case Some(c) =>
          Ok(views.html.exam.index(c, examApi.students, examApi.exams))
        case None => Ok(views.html.index())
      }
    }
  }

}

