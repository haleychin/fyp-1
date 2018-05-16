package models

import javax.inject.{ Inject, Singleton }
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import scala.util.{Try}

import java.sql.Timestamp
import slick.driver.PostgresDriver.api._

import scala.concurrent.{ Future, ExecutionContext }

case class CourseStudent(courseId: Long, studentId: Long, createdAt: Timestamp, updateAt: Timestamp)

@Singleton
class CourseStudentRepository @Inject() (
  dbConfigProvider: DatabaseConfigProvider,
  courseRepository: CourseRepository,
  studentRepository: StudentRepository
 )(implicit ec: ExecutionContext) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._    // Bring db in scope
  import profile.api._ // Slick DSL

  private val coursesTable = TableQuery[courseRepository.CourseTable]
  private val studentsTable = TableQuery[studentRepository.StudentTable]

  // Define table
  private class CourseStudentTable(tag: Tag) extends Table[CourseStudent](tag, "courses_students") {

    // Define columns
    def courseId = column[Long]("course_id")
    def studentId = column[Long]("student_id")
    def createdAt = column[Timestamp]("created_at", O.SqlType("timestamp default now()"))
    def updatedAt = column[Timestamp]("updated_at", O.SqlType("timestamp default now()"))
    def pk = primaryKey("primary_key", (courseId, studentId))

    def courses = foreignKey("fk_courses", courseId, coursesTable)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
    def students = foreignKey("fk_students", studentId, studentsTable)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

    // Default Projection
    def * = (courseId, studentId, createdAt, updatedAt) <> (CourseStudent.tupled, CourseStudent.unapply)
  }

  private val coursesStudents = TableQuery[CourseStudentTable]

  // Print SQL command to create table
  // coursesStudents.schema.create.statements.foreach(println)

  def create(courseId: Long, studentId: Long): Future[CourseStudent] = {
    val seq = (
    (coursesStudents.map(cs => (cs.courseId, cs.studentId))
      returning coursesStudents.map(cs => (cs.createdAt, cs.updatedAt))
      into ((form, cs) => CourseStudent(form._1, form._2, cs._1, cs._2))
      ) += (courseId, studentId)
    )
    db.run(seq)
  }

  def delete(courseId: Long, studentId: Long): Future[Int] = {
    val action = coursesStudents.filter(cs =>  cs.courseId === courseId && cs.studentId === studentId).delete
    db.run(action)
  }
}
