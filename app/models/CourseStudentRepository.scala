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
  val courseRepository: CourseRepository,
  val studentRepository: StudentRepository
 )(implicit ec: ExecutionContext) {
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._    // Bring db in scope
  import profile.api._ // Slick DSL

  // Define table
  class CourseStudentTable(tag: Tag) extends Table[CourseStudent](tag, "courses_students") {

    // Define columns
    def courseId = column[Long]("course_id")
    def studentId = column[Long]("student_id")
    def createdAt = column[Timestamp]("created_at", O.SqlType("timestamp default now()"))
    def updatedAt = column[Timestamp]("updated_at", O.SqlType("timestamp default now()"))
    def pk = primaryKey("primary_key", (courseId, studentId))

    def courses = foreignKey("fk_courses", courseId, courseRepository.courses)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
    def students = foreignKey("fk_students", studentId, studentRepository.students)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

    // Default Projection
    def * = (courseId, studentId, createdAt, updatedAt) <> (CourseStudent.tupled, CourseStudent.unapply)
  }

  val coursesStudents = TableQuery[CourseStudentTable]

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

  def getStudents(courseId: Long): Future[Seq[Student]] = {
    val query = for {
      (s, c) <- studentRepository.students join coursesStudents on (_.id === _.courseId)
    } yield s
    val result = db.run(query.result)
    result
  }

  def delete(courseId: Long, studentId: Long): Future[Int] = {
    val action = coursesStudents.filter(cs =>  cs.courseId === courseId && cs.studentId === studentId).delete
    db.run(action)
  }
}
