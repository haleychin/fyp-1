package controllers

import javax.inject._
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.collection.mutable.LinkedHashMap

// For Files
import java.nio.file.Paths
import java.io.File

// Model
import models._
import utils._

case class ExamMetricAPI(
  course: Option[Course],
  students: Seq[Student],
  exams: ExamAPI,
  metrics: LinkedHashMap[String,MetricStat]
)

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

  def getExamDetails(courseId: Long): Future[ExamMetricAPI] = {
    val courseFuture = cRepo.get(courseId)
    val studentFuture = csRepo.getStudents(courseId)
    val examsFuture = repo.getExams(courseId)
    val metricFuture = mRepo.getMetrics(courseId)

    val results = for {
      course <- courseFuture
      students <- studentFuture
      exams <- examsFuture
      metric <- metricFuture
    } yield (course, students, exams, metric)


    results.map { r =>
      val result = Analyser.analyseExam(r._4)
      ExamMetricAPI(r._1, r._2, r._3, result)
    }
  }

  def index(courseId: Long) = Action.async { implicit request =>
    getExamDetails(courseId).map { examApi =>
      examApi.course match {
        case Some(c) =>
          Ok(views.html.examination.index(c, examApi.students, examApi.exams, examApi.metrics))
        case None => Ok(views.html.index())
      }
    }
  }

}

