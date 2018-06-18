package models

import javax.inject.{ Inject, Singleton }
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import java.sql.Timestamp
import slick.driver.PostgresDriver.api._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

case class Question(id: Long, examId: Long, courseId: Long,
  name: String, totalMark: Double, mark: Double,
  createdAt: Timestamp, updateAt: Timestamp)

@Singleton
class QuestionRepository @Inject() (
  dbConfigProvider: DatabaseConfigProvider,
  val sRepo: StudentRepository,
  val eRepo: ExamRepository
)(implicit ec: ExecutionContext) {
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._    // Bring db in scope
  import profile.api._ // Slick DSL

  class QuestionTable(tag: Tag) extends Table[Question](tag, "questions") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def examId = column[Long]("exam_id")
    def courseId = column[Long]("course_id")
    def name = column[String]("name")
    def totalMark = column[Double]("totalMark")
    def mark = column[Double]("mark")
    def createdAt = column[Timestamp]("created_at", O.SqlType("timestamp default now()"))
    def updatedAt = column[Timestamp]("updated_at", O.SqlType("timestamp default now()"))

    def idx = index("question_unique", (examId, name), unique = true)
    def exams = foreignKey("fk_exams", examId, eRepo.exams)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

    def * = (id, examId, courseId, name, totalMark, mark, createdAt, updatedAt) <> (Question.tupled, Question.unapply)
  }

  val questions = TableQuery[QuestionTable]
  // questions.schema.create.statements.foreach(println)

  def create(examId: Long, courseId: Long, name: String, totalMark: Double,
    mark: Double): Future[Option[Question]] = {
    eRepo.get(examId).flatMap { s =>
      s match {
        case Some(student) =>
          val seq = (
          (questions.map(q => (q.examId, q.courseId, q.name, q.totalMark, q.mark))
            returning questions.map(q => (q.id, q.createdAt, q.updatedAt))
            into ((question, q) => Question(q._1, question._1, question._2, question._3, question._4, question._5, q._2, q._3))
            ) += (examId, courseId, name, totalMark, mark)
          )
          val result = db.run(seq)
          result.map(Some(_))
        case None =>
          println("Cant find student")
          Future(None)
      }
    }
  }

}

