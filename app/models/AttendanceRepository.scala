package models

import javax.inject.{ Inject, Singleton }
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import scala.util.{Try}

import java.sql.{Timestamp, Date}
import slick.driver.PostgresDriver.api._

import scala.concurrent.{ExecutionContext, Future, Await}
import scala.concurrent.duration._

case class Attendance(id: Long, courseId: Long, studentId: Long, date: Date,
  attendanceType: String, createdAt: Timestamp, updateAt: Timestamp)

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
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def courseId = column[Long]("course_id")
    def studentId = column[Long]("student_id")
    def attendanceType = column[String]("attendance_type")
    def date = column[Date]("date")
    def createdAt = column[Timestamp]("created_at", O.SqlType("timestamp default now()"))
    def updatedAt = column[Timestamp]("updated_at", O.SqlType("timestamp default now()"))

    def courses = foreignKey("fk_courses", courseId, cRepo.courses)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
    def students = foreignKey("fk_students", studentId, sRepo.students)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

    // Default Projection
    def * = (id, courseId, studentId, date, attendanceType, createdAt, updatedAt) <> (Attendance.tupled, Attendance.unapply)
  }

  val attendances = TableQuery[AttendanceTable]

  // Print SQL command to create table
  // attendances.schema.create.statements.foreach(println)

  // studentId here refer to the Student Id for student instead
  // of the primary key of the Student record
  def create(courseId: Long, studentId: String, date: Date, attendanceType: String): Future[Attendance] = {
    // Blocking and Force Unwrap here
    val student = Await.result(sRepo.getByStudentId(studentId).map(_.get), 1 second)
    val seq = (
    (attendances.map(a => (a.courseId, a.studentId, a.date, a.attendanceType))
      returning attendances.map(a => (a.id, a.createdAt, a.updatedAt))
      into ((form, a) => Attendance(a._1, form._1, form._2, form._3,
        form._4, a._2, a._3))
      ) += (courseId, student.id, date, attendanceType)
    )
    db.run(seq)
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
