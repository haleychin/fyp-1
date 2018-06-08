package models

import javax.inject.{ Inject, Singleton }
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import java.sql.Timestamp
import slick.driver.PostgresDriver.api._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

case class Question(id: Long, examId: Long,  name: String,
  totalMark: Double, mark: Double, createdAt: Timestamp,
  updateAt: Timestamp)

@Singleton
class QuestionRepository @Inject() (
  dbConfigProvider: DatabaseConfigProvider
)(implicit ec: ExecutionContext) {
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._    // Bring db in scope
  import profile.api._ // Slick DSL

  class QuestionTable(tag: Tag) extends Table[Question](tag, "questions") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def examId = column[Long]("exam_id")
    def name = column[String]("name")
    def totalMark = column[Double]("totalMark")
    def mark = column[Double]("mark")
    def createdAt = column[Timestamp]("created_at", O.SqlType("timestamp default now()"))
    def updatedAt = column[Timestamp]("updated_at", O.SqlType("timestamp default now()"))

    def * = (id, examId, name, mark, totalMark, createdAt, updatedAt) <> (Question.tupled, Question.unapply)
  }

  val questions = TableQuery[QuestionTable]

  def create(examId: Long, name: String, totalMark: Double, mark: Double): Future[Question] = {
    val seq = (
    (questions.map(q => (q.examId, q.name, q.totalMark, q.mark))
      returning questions.map(q => (q.id, q.createdAt, q.updatedAt))
      into ((question, q) => Question(q._1, question._1, question._2,
        question._3, question._4, q._2, q._3))
      ) += (examId, name, totalMark, mark)
    )
    db.run(seq)
  }
}

