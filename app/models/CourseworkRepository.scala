package models

import javax.inject.{ Inject, Singleton }
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import scala.util.{Success, Failure}

import java.sql.Timestamp
import slick.driver.PostgresDriver.api._

import scala.concurrent.{ExecutionContext, Future, Await}
import scala.concurrent.duration._

import scala.collection.mutable.{LinkedHashMap, LinkedHashSet}

case class CwStatistic(
  averages: LinkedHashMap[String, Double],
  passCount: Int,
  failCount: Int)

case class CourseworkAPI(
  courseworkDetails: Iterable[CourseworkDetailsAPI],
  courseworks: LinkedHashSet[(String, Double)],
  total: Double,
  statistic: CwStatistic)
case class CourseworkDetailsAPI(
  student: Student,
  var courseworks: LinkedHashMap[String, Double],
  var total: Double,
  var status: String)

case class Coursework(courseId: Long, studentId: Long, name: String,
  mark: Double, totalMark: Double, createdAt: Timestamp,
  updateAt: Timestamp)

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
    def totalMark = column[Double]("total_mark")
    def createdAt = column[Timestamp]("created_at", O.SqlType("timestamp default now()"))
    def updatedAt = column[Timestamp]("updated_at", O.SqlType("timestamp default now()"))
    def pk = primaryKey("pk_coursework", (courseId, studentId, name))

    def courses = foreignKey("cw_fk_courses", courseId, cRepo.courses)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
    def students = foreignKey("cw_fk_students", studentId, sRepo.students)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

    // Default Projection
    def * = (courseId, studentId, name, mark, totalMark, createdAt, updatedAt) <> (Coursework.tupled, Coursework.unapply)
  }

  val courseworks = TableQuery[CourseworkTable]

  // studentId here refer to the Student Id for student instead
  // of the primary key of the Student record
  def create(courseId: Long, studentId: String, name: String,
    mark: Double, totalMark: Double): Future[Option[Coursework]] = {
    sRepo.getByStudentId(studentId).flatMap { s =>
      s match {
        case Some(student) =>
          val seq = (
            (courseworks.map(a => (
              a.courseId, a.studentId, a.name, a.mark, a.totalMark))
              returning courseworks.map(a => (a.createdAt, a.updatedAt))
              into ((form, a) => Coursework(
                form._1, form._2, form._3, form._4, form._5,
                a._1, a._2))
            ) += (courseId, student.id, name, mark, totalMark)
          )
          val result = db.run(seq.asTry)
          result.map { r =>
            println(r)
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
          println("Cant find student")
          Future(None)
      }
    }
  }

  def getCourseworks(courseId: Long): Future[CourseworkAPI] = {
    val query = for {
      cw <- courseworks
      courses <- cw.courses if courses.id === courseId
      students <- cw.students
    } yield (students, cw)

    val result = db.run(query.result)

    var studentMap = LinkedHashMap[Long, CourseworkDetailsAPI]()
    val courseworkLists = LinkedHashSet[(String, Double)]()

    result.map { r =>
      r.foreach { case (student, cw) =>
        courseworkLists += ((cw.name, cw.totalMark))
        val value = cw.name -> cw.mark
        if (studentMap.contains(student.id)) {
          val s = studentMap.get(student.id).get
          s.courseworks += value
          s.total += cw.mark
        } else {
          val data = LinkedHashMap[String, Double](value)
          studentMap += (student.id -> CourseworkDetailsAPI(student, data, cw.mark, ""))
        }
      }

      val total = courseworkLists.reduce( (x, y) =>
        ("", x._2 + y._2)
      )._2

      studentMap.foreach { case (_, s) =>
        s.status = calculatePass(s.total, total)
      }

      val statistic = computeStatistic(studentMap.values)

      CourseworkAPI(studentMap.values, courseworkLists, total, statistic)
    }
  }

  def computeStatistic(data: Iterable[CourseworkDetailsAPI]): CwStatistic = {
    var averages = LinkedHashMap[String, Double]()
    val size = data.size
    var passCount = 0;

    data.foreach { d =>
      val courseworksMap = d.courseworks

      d.courseworks.foreach { case (key, value) =>
        if (averages.contains(key)) {
          averages.update(key, averages.get(key).get + value)
        } else {
          averages += (key -> value)
        }
      }

      if (d.status == "Pass") { passCount += 1 }
    }


    val total = data.map(_.total).reduce(_ + _)
    averages += ("Total" -> total)
    val failCount = size - passCount

    averages.foreach { case (k, v) => averages.update(k, v / data.size) }

    CwStatistic(averages, passCount, failCount)
  }

  def calculatePass(weightage: Double, totalWeightage: Double): String = {
    val rate = weightage / totalWeightage * 100
    if (rate <= 40) {
      "Fail"
    } else {
      "Pass"
    }
  }

}
