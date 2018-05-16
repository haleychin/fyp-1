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
  coursesStudents.schema.create.statements.foreach(println)

  // =================
  // Define CRUD here.
  // =================
  // def list(): Future[Seq[Course]] = db.run {
  //   courses.result
  // }

  // def get(id: Long): Future[Option[Course]] = db.run {
  //   courses.filter(_.id === id).result.headOption
  // }

  // // Create Course
  // def create(title: String, userId: Long): Future[Course] = {
  //   val seq = (
  //   (courses.map(u => (u.userId, u.title))
  //     returning courses.map(c => (c.id, c.createdAt, c.updatedAt))
  //     into ((course, c) => Course(c._1, course._1, course._2, c._2, c._3))
  //     ) += (userId, title)
  //   )
  //   db.run(seq)
  // }

  // def update(id: Long, title: String): Future[Int] = {
  //   val course = courses.filter(_.id === id)
  //   val action = course.map(u => (u.title)).update(title)
  //   db.run(action)
  // }

  // def delete(id: Long): Future[Int] = {
  //   val action = courses.filter(_.id === id).delete
  //   db.run(action)
  // }
}
