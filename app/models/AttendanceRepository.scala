package models

import javax.inject.{ Inject, Singleton }
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import scala.util.{Success, Failure}

import java.sql.{Timestamp, Date}
import slick.driver.PostgresDriver.api._

import scala.concurrent.{ExecutionContext, Future, Await}
import scala.concurrent.duration._

case class Attendance(courseId: Long, studentId: Long, groupId: Int,
  date: Date, attendanceType: String, createdAt: Timestamp,
  updateAt: Timestamp)

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
    def groupId = column[Int]("group_id")
    def attendanceType = column[String]("attendance_type")
    def date = column[Date]("date")
    def createdAt = column[Timestamp]("created_at", O.SqlType("timestamp default now()"))
    def updatedAt = column[Timestamp]("updated_at", O.SqlType("timestamp default now()"))
    def pk = primaryKey("pk_attendance", (courseId, studentId, groupId, date))

    def courses = foreignKey("fk_courses", courseId, cRepo.courses)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
    def students = foreignKey("fk_students", studentId, sRepo.students)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

    // Default Projection
    def * = (courseId, studentId, groupId, date, attendanceType, createdAt, updatedAt) <> (Attendance.tupled, Attendance.unapply)
  }

  val attendances = TableQuery[AttendanceTable]

  // Print SQL command to create table
  attendances.schema.create.statements.foreach(println)

  // studentId here refer to the Student Id for student instead
  // of the primary key of the Student record
  def create(courseId: Long, studentId: String, groupId: Int, date: Date, attendanceType: String): Future[Option[Attendance]] = {
    sRepo.getByStudentId(studentId).flatMap { s =>
      s match {
        case Some(student) =>
          val seq = (
          (attendances.map(a => (a.courseId, a.studentId, a.groupId, a.date, a.attendanceType))
            returning attendances.map(a => (a.createdAt, a.updatedAt))
            into ((form, a) => Attendance(form._1, form._2, form._3,
              form._4, form._5, a._1, a._2))
            ) += (courseId, student.id, groupId, date, attendanceType)
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
