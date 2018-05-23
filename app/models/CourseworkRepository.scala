package models

import javax.inject.{ Inject, Singleton }
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import scala.util.{Success, Failure}

import java.sql.Timestamp
import slick.driver.PostgresDriver.api._

import scala.concurrent.{ExecutionContext, Future, Await}
import scala.concurrent.duration._

case class Coursework(courseId: Long, studentId: Long, name: String,
  mark: Double, createdAt: Timestamp, updateAt: Timestamp)

@Singleton
class CourseworkRepository @Inject() (
  dbConfigProvider: DatabaseConfigProvider,
  val cRepo: CourseRepository,
  val sRepo: StudentRepository
 )(implicit ec: ExecutionContext) {
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._    // Bring db in scope
  import profile.api._ // Slick DSL

  // Define table
  class CourseworkTable(tag: Tag) extends Table[Coursework](tag, "courseworks") {

    // Define columns
    def courseId = column[Long]("course_id")
    def studentId = column[Long]("student_id")
    def name = column[String]("name")
    def mark = column[Double]("mark")
    def createdAt = column[Timestamp]("created_at", O.SqlType("timestamp default now()"))
    def updatedAt = column[Timestamp]("updated_at", O.SqlType("timestamp default now()"))
    def pk = primaryKey("pk_coursework", (courseId, studentId, name))

    def courses = foreignKey("cw_fk_courses", courseId, cRepo.courses)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
    def students = foreignKey("cw_fk_students", studentId, sRepo.students)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

    // Default Projection
    def * = (courseId, studentId, name, mark, createdAt, updatedAt) <> (Coursework.tupled, Coursework.unapply)
  }

  val courseworks = TableQuery[CourseworkTable]

  // Print SQL command to create table
  courseworks.schema.create.statements.foreach(println)

  // studentId here refer to the Student Id for student instead
  // of the primary key of the Student record
  def create(courseId: Long, studentId: String, name: String, mark: Double): Future[Option[Coursework]] = {
    sRepo.getByStudentId(studentId).flatMap { s =>
      s match {
        case Some(student) =>
          val seq = (
          (courseworks.map(a => (a.courseId, a.studentId, a.name, a.mark))
            returning courseworks.map(a => (a.createdAt, a.updatedAt))
            into ((form, a) => Coursework(form._1, form._2, form._3, form._4,
              a._1, a._2))
            ) += (courseId, student.id, name, mark)
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

  // def getStudents(courseId: Long): Future[Seq[Student]] = {
  //   val query = for {
  //     cs <- attedances
  //     courses <- cs.courses if courses.id === courseId
  //     students <- cs.students
  //   } yield students

  //   val result = db.run(query.result)
  //   result
  // }

  // def getCourses(studentId: Long): Future[Seq[Course]] = {
  //   val query = for {
  //     cs <- attedances
  //     students <- cs.students if students.id === studentId
  //     courses <- cs.courses
  //   } yield courses

  //   val result = db.run(query.result)
  //   result
  // }

}
