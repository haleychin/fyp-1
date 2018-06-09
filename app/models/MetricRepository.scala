package models

import javax.inject.{ Inject, Singleton }
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import java.sql.Timestamp
import slick.driver.PostgresDriver.api._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

case class Metric(id: Long, courseId: Long, name: String,
  description: String, createdAt: Timestamp, updateAt: Timestamp)
case class QuestionMetric(questionId: Long, metricId: Long,
  createdAt: Timestamp, updateAt: Timestamp)

@Singleton
class MetricRepository @Inject() (
  dbConfigProvider: DatabaseConfigProvider,
  val questionRepo: QuestionRepository
)(implicit ec: ExecutionContext) {
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._    // Bring db in scope
  import profile.api._ // Slick DSL

  // ======
  // Metric
  // ======
  class MetricTable(tag: Tag) extends Table[Metric](tag, "metrics") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def courseId = column[Long]("course_id")
    def name = column[String]("name")
    def description = column[String]("description")
    def createdAt = column[Timestamp]("created_at", O.SqlType("timestamp default now()"))
    def updatedAt = column[Timestamp]("updated_at", O.SqlType("timestamp default now()"))

    def * = (id, courseId, name, description, createdAt, updatedAt) <> (Metric.tupled, Metric.unapply)
  }
  val metricsTable = TableQuery[MetricTable]


  // ==============
  // QuestionMetric
  // ==============
  class QuestionMetricTable(tag: Tag) extends Table[QuestionMetric](tag, "questions_metrics") {
    def questionId = column[Long]("question_id")
    def metricId = column[Long]("metric_id")
    def createdAt = column[Timestamp]("created_at", O.SqlType("timestamp default now()"))
    def updatedAt = column[Timestamp]("updated_at", O.SqlType("timestamp default now()"))
    def pk = primaryKey("qm_primary_key", (questionId, metricId))

    def questions = foreignKey("fk_questions", questionId, questionRepo.questions)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
    def metrics = foreignKey("fk_metrics", metricId, metricsTable)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

    // Default Projection
    def * = (questionId, metricId, createdAt, updatedAt) <> (QuestionMetric.tupled, QuestionMetric.unapply)
  }
  val questionMetrics = TableQuery[QuestionMetricTable]

  def create(questionId: Long, metricId: Long): Future[QuestionMetric] = {
    val seq = (
    (questionMetrics.map(qm => (qm.questionId, qm.metricId))
      returning questionMetrics.map(qm => (qm.createdAt, qm.updatedAt))
      into ((form, qm) => QuestionMetric(form._1, form._2, qm._1, qm._2))
      ) += (questionId, metricId)
    )
    db.run(seq)
  }

  def create(courseId: Long, name: String, description: String): Future[Metric] = {
    val seq = (
    (metricsTable.map(u => (u.courseId, u.name, u.description))
      returning metricsTable.map(d => (d.id, d.createdAt, d.updatedAt))
      into ((metric, d) => Metric(d._1, metric._1, metric._2, metric._3,
        d._2, d._3))
      ) += (courseId, name, description)
    )
    db.run(seq)
  }

  // def getMetric(courseId: Long, name: String): Future[Metric] = {
  // }
}

