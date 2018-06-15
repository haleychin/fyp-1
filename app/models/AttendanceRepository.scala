package models

import javax.inject.{ Inject, Singleton }
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import scala.util.{Success, Failure}

import java.sql.{Timestamp, Date}
import slick.driver.PostgresDriver.api._

import scala.concurrent.{ExecutionContext, Future, Await}
import scala.concurrent.duration._

import scala.collection.mutable.{ArrayBuffer, LinkedHashMap, LinkedHashSet}
import utils.Analyser

case class Attendance(courseId: Long, studentId: Long, groupId: Int,
  date: Date, attendanceType: String, createdAt: Timestamp,
  updateAt: Timestamp)

@Singleton
class AttendanceRepository @Inject() (
  dbConfigProvider: DatabaseConfigProvider,
  val cRepo: CourseRepository,
  val sRepo: StudentRepository,
  val fsRepo: FilterSettingRepository,
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
                Some(a)
              case Failure(e) =>
                None
            }
          }
        case None =>
          Future(None) }
    }

  }

  def getAttendances(
    courseId: Long,
    programme: String = "%",
    intake: String = "%"): Future[AttendanceAPI] = {
    val query = (for {
      a <- attendances
      courses <- a.courses if courses.id === courseId
      students <- a.students if (students.programme like programme) &&   (students.intake like intake)
    } yield (students, a)).sortBy(_._2.date)

    val result = db.run(query.result)
    val setting = fsRepo.get(courseId)

    var studentMap = LinkedHashMap[Long, StudentDetailsAPI]()
    val groupIdDates = LinkedHashSet[(Int, Date)]()

    setting.flatMap { filter =>
      result.map { r =>
        r.foreach { case (student, a) =>
          groupIdDates += ((a.groupId, a.date))
          if (studentMap.contains(student.id)) {
            // println(s"Inserting ${student.name} attendance on ${a.date}")
            studentMap.get(student.id).get.attendances += (a.date -> a.attendanceType)
          } else {
            val data = LinkedHashMap[Date, String](a.date -> a.attendanceType)
            studentMap += (student.id -> StudentDetailsAPI(student, data))
          }
        }

        studentMap.foreach { case (_, s) =>
          s.stat = calculateRate(s.attendances)
          // WARNING: Force unwrap FilterSetting here
          s.insight = Analyser.analyseAttendance(s.stat, filter.get)
        }

        AttendanceAPI(studentMap, groupIdDates)
      }
    }
  }

  def getCoursesAttendance(studentId: Long): Future[CAttendanceAPI] = {
    val query = (for {
      a <- attendances
      courses <- a.courses
      students <- a.students if students.id === studentId
    } yield (courses, a)).sortBy(_._2.date)

    val result = db.run(query.result)
    var courseMap = LinkedHashMap[Long, CourseDetailsAPI]()

    result.map { r =>
      r.foreach { case (course, a) =>
        if (courseMap.contains(course.id)) {
          courseMap.get(course.id).get.attendances += (a.date -> a.attendanceType)
        } else {
          val data = LinkedHashMap[Date, String](a.date -> a.attendanceType)
          courseMap += (course.id -> CourseDetailsAPI(course, data, 0.0))
        }
      }

      courseMap.foreach { case (_, c) =>
        c.attendanceRate = oldCalculateRate(c.attendances)
      }

      CAttendanceAPI(courseMap)
    }

  }

  def oldCalculateRate(attendances: LinkedHashMap[Date,String]): Double = {
    var attend = 0.0
    attendances.foreach { case (_, value) =>
      if (value == "attend") {
        attend += 1
      }
    }

     attend / attendances.size * 100
  }

  def calculateRate(attendances: LinkedHashMap[Date,String]): AStat = {
    var attend = 0.0
    var absent = 0
    var previous = ""
    val consecutiveMissed = ArrayBuffer[ArrayBuffer[Date]]()

    attendances.foreach { case (date, value) =>
      if (value == "attend") {
        previous = "attend"
        attend += 1

      } else {
        if (consecutiveMissed.isEmpty || previous == "attend") {
          consecutiveMissed += ArrayBuffer(date)
        }
        if (previous == "absent") {
          consecutiveMissed.last += date
        }
        absent += 1
        previous = "absent"
      }
    }

    AStat(
      absent,
      attend.toInt,
      attend / attendances.size * 100,
      consecutiveMissed)
  }
 }
