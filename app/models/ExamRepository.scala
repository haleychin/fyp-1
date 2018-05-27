package models

import javax.inject.{ Inject, Singleton }
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import scala.util.{Success, Failure}

import java.sql.Timestamp
import slick.driver.PostgresDriver.api._

import scala.concurrent.{ExecutionContext, Future, Await}
import scala.concurrent.duration._

case class Exam(courseId: Long, studentId: Long,
  mark: Double, totalMark: Double,
  weightage: Double, totalWeightage: Double,
  createdAt: Timestamp, updateAt: Timestamp)

@Singleton
class ExamRepository @Inject() (
  dbConfigProvider: DatabaseConfigProvider,
  val cRepo: CourseRepository,
  val sRepo: StudentRepository
)(implicit ec: ExecutionContext) {
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._    // Bring db in scope
  import profile.api._ // Slick DSL

  // Define table
  class ExamTable(tag: Tag) extends Table[Exam](tag, "exams") {

    // Define columns
    def courseId = column[Long]("course_id")
    def studentId = column[Long]("student_id")
    def mark = column[Double]("mark")
    def totalMark = column[Double]("total_mark")
    def weightage = column[Double]("weightage")
    def totalWeightage = column[Double]("total_weightage")
    def createdAt = column[Timestamp]("created_at", O.SqlType("timestamp default now()"))
    def updatedAt = column[Timestamp]("updated_at", O.SqlType("timestamp default now()"))
    def pk = primaryKey("pk_exam", (courseId, studentId))

    def courses = foreignKey("e_fk_courses", courseId, cRepo.courses)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
    def students = foreignKey("e_fk_students", studentId, sRepo.students)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

    // Default Projection
    def * = (courseId, studentId, mark, totalMark, weightage, totalWeightage, createdAt, updatedAt) <> (Exam.tupled, Exam.unapply)
  }

  val exams = TableQuery[ExamTable]
  exams.schema.create.statements.foreach(println)

  // studentId here refer to the Student Id for student instead
  // of the primary key of the Student record
  def create(courseId: Long, studentId: String,
    mark: Double, totalMark: Double,
    weightage: Double, totalWeightage: Double): Future[Option[Exam]] = {
      sRepo.getByStudentId(studentId).flatMap { s =>
        s match {
          case Some(student) =>
            val seq = (
              (exams.map(a => (
                a.courseId, a.studentId, a.mark, a.totalMark, a.weightage, a.totalWeightage))
            returning exams.map(a => (a.createdAt, a.updatedAt))
            into ((form, a) => Exam(
              form._1, form._2, form._3, form._4, form._5, form._6, a._1, a._2))
          ) += (courseId, student.id, mark, totalMark, weightage, totalWeightage)
        )
            val result = db.run(seq.asTry)
            result.map { r =>
              r match {
                case Success(a) =>
                  println(a)
                  Some(a)
                case Failure(e) =>
                  println(e)
                  None
              }
            }
          case None =>
            Future(None)
        }
      }

  }
}
