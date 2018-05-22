package models

import javax.inject.{ Inject, Singleton }
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import scala.util.{Try}

import java.sql.Timestamp
import slick.driver.PostgresDriver.api._

import scala.concurrent.{ExecutionContext, Future, Await}
import scala.concurrent.duration._

case class Attendance(courseId: Long, studentId: Long, attendanceType: String, createdAt: Timestamp, updateAt: Timestamp)

@Singleton
class AttendanceRepository @Inject() (
  dbConfigProvider: DatabaseConfigProvider,
  val cRepo: CourseRepository,
  val sRepo: StudentRepository
 )(implicit ec: ExecutionContext) {
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._    // Bring db in scope
  import profile.api._ // Slick DSL

  // Define table
  class AttendanceTable(tag: Tag) extends Table[Attendance](tag, "attendances") {

    // Define columns
    def courseId = column[Long]("course_id")
    def studentId = column[Long]("student_id")
    def attendanceType = column[String]("attendance_type")
    def createdAt = column[Timestamp]("created_at", O.SqlType("timestamp default now()"))
    def updatedAt = column[Timestamp]("updated_at", O.SqlType("timestamp default now()"))
    def pk = primaryKey("primary_key", (courseId, studentId))

    def courses = foreignKey("fk_courses", courseId, cRepo.courses)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
    def students = foreignKey("fk_students", studentId, sRepo.students)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

    // Default Projection
    def * = (courseId, studentId, attendanceType, createdAt, updatedAt) <> (Attendance.tupled, Attendance.unapply)
  }

  val attendances = TableQuery[AttendanceTable]

  // Print SQL command to create table
  attendances.schema.create.statements.foreach(println)

  def create(courseId: Long, studentId: Long, attendanceType: String): Future[Attendance] = {
    val seq = (
    (attendances.map(a => (a.courseId, a.studentId, attendanceType))
      returning attendances.map(a => (a.createdAt, a.updatedAt))
      into ((form, a) => Attendance(form._1, form._2, form._3, a._1, a._2))
      ) += (courseId, studentId, attendanceType)
    )
    db.run(seq)
  }

  // def createWithStudentId(courseId: Long, studentId: String): Future[Attendance] = {
  //   // Blocking and Force Unwrap here
  //   val student = Await.result(sRepo.getByStudentId(studentId).map(_.get), 1 second)
  //   create(courseId, student.id)
  // }

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
