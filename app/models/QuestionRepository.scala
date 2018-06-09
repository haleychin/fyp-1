package models

import javax.inject.{ Inject, Singleton }
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import java.sql.Timestamp
import slick.driver.PostgresDriver.api._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

case class Question(id: Long, courseId: Long, studentId: Long,
  name: String, totalMark: Double, mark: Double,
  createdAt: Timestamp, updateAt: Timestamp)

@Singleton
class QuestionRepository @Inject() (
  dbConfigProvider: DatabaseConfigProvider,
  val  sRepo: StudentRepository
)(implicit ec: ExecutionContext) {
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._    // Bring db in scope
  import profile.api._ // Slick DSL

  class QuestionTable(tag: Tag) extends Table[Question](tag, "questions") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def courseId = column[Long]("course_id")
    def studentId = column[Long]("student_id")
    def name = column[String]("name")
    def totalMark = column[Double]("totalMark")
    def mark = column[Double]("mark")
    def createdAt = column[Timestamp]("created_at", O.SqlType("timestamp default now()"))
    def updatedAt = column[Timestamp]("updated_at", O.SqlType("timestamp default now()"))

    def * = (id, courseId, studentId, name, mark, totalMark, createdAt, updatedAt) <> (Question.tupled, Question.unapply)
  }

  val questions = TableQuery[QuestionTable]

  def create(courseId: Long, studentId: String,
    name: String, totalMark: Double,
    mark: Double): Future[Option[Question]] = {
    sRepo.getByStudentId(studentId).flatMap { s =>
      s match {
        case Some(student) =>
          val seq = (
          (questions.map(q => (q.courseId, q.studentId, q.name, q.totalMark, q.mark))
            returning questions.map(q => (q.id, q.createdAt, q.updatedAt))
            into ((question, q) => Question(q._1, question._1, question._2,
              question._3, question._4, question._5, q._2, q._3))
            ) += (courseId, student.id, name, totalMark, mark)
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

