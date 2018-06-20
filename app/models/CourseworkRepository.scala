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
import utils._

case class Coursework(courseId: Long, studentId: Long, name: String,
  mark: Double, totalMark: Double, createdAt: Timestamp,
  updateAt: Timestamp)

@Singleton
class CourseworkRepository @Inject() (
  dbConfigProvider: DatabaseConfigProvider,
  val cRepo: CourseRepository,
  val sRepo: StudentRepository,
  val fsRepo: FilterSettingRepository,
 )(implicit ec: ExecutionContext) {
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._    // Bring db in scope import profile.api._ // Slick DSL

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

  def delete(courseId: Long): Future[Int] = db.run {
    courseworks.filter(_.courseId === courseId).delete
  }

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

  def getCourseworks(
    courseId: Long,
    programme: String = "%",
    intake: String = "%"): Future[CourseworkAPI] = {
    val query = for {
      cw <- courseworks
      courses <- cw.courses if courses.id === courseId
      students <- cw.students if (students.programme like programme) &&   (students.intake like intake)
    } yield (students, cw)

    val result = db.run(query.result)
    val setting = fsRepo.get(courseId)

    var studentMap = LinkedHashMap[Long, CourseworkDetailsAPI]()
    // (Name, Total Mark)
    val courseworkLists = LinkedHashSet[(String, Double)]()

    setting.flatMap { filter =>
      result.map { r =>
        r.foreach { case (student, cw) =>
          courseworkLists += ((cw.name, cw.totalMark))
          val value = cw.name -> cw.mark
          val totalValue = cw.name -> cw.totalMark
          if (studentMap.contains(student.id)) {
            val s = studentMap.get(student.id).get
            s.courseworks += value
            s.courseworksTotal += totalValue
            s.total += cw.mark
          } else {
            val data = LinkedHashMap[String,Double](value)
            val totalData = LinkedHashMap[String,Double](totalValue)
            studentMap += (student.id -> CourseworkDetailsAPI(student, data, totalData, cw.mark))
          }
        }

        val total = courseworkLists.reduceOption( (x, y) =>
            ("", x._2 + y._2)
          ).map(_._2).getOrElse(0.0)

        studentMap.foreach { case (_, s) =>
          s.status = Utils.calculatePass(s.total, total)
          val gradeTotal = Utils.calculatePercent(s.total, total)
          s.grade  = Utils.calculateStatus(0.0, 0.0, gradeTotal, false)
          // WARNING: Force unwrap FilterSetting here
          s.insight = Analyser.analyseCoursework(s, filter.get)
        }

        val statistic = computeStatistic(studentMap.values)
        val marks = studentMap.values.map(_.total).toSeq
        val descStat  = Stats.computeDescriptiveStatistic(marks)

        CourseworkAPI(studentMap, courseworkLists, total, statistic, descStat)
      }
    }
  }

  def getCoursesCourseworks(studentId: Long): Future[CCourseworkAPI] = {
    val query = for {
      cw <- courseworks
      courses <- cw.courses
      students <- cw.students if students.id === studentId
    } yield (courses, cw)

    val result = db.run(query.result)
    var courseMap = LinkedHashMap[Long, CCourseworkDetailsAPI]()

    result.map { r =>
      r.foreach { case (course, cw) =>
        val value = cw.name -> cw.mark
        val totalValue = cw.name -> cw.totalMark
        if (courseMap.contains(course.id)) {
          val s = courseMap.get(course.id).get
          s.courseworks += value
          s.courseworksTotal += totalValue
          s.totalMark += cw.mark
          s.fullMark += cw.totalMark
        } else {
          val data = LinkedHashMap[String, Double](value)
          val totalData = LinkedHashMap[String,Double](totalValue)
          courseMap += (course.id -> CCourseworkDetailsAPI(course, data, totalData, cw.mark, cw.totalMark, ""))
        }
      }

      courseMap.foreach { case (_, s) =>
        s.status = Utils.calculatePass(s.totalMark, s.fullMark)
      }

      val statistic = computeCourseStatistic(courseMap.values)
      val marks = courseMap.values.map(_.totalMark).toSeq
      val descStat  = Stats.computeDescriptiveStatistic(marks)

      CCourseworkAPI(courseMap, statistic, descStat)
    }
  }

  def computeCourseStatistic(data: Iterable[CCourseworkDetailsAPI]): CwStatistic = {
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


    val total = data.map(_.totalMark).reduceOption(_ + _).getOrElse(0.0)
    averages += ("Total" -> total)
    val failCount = size - passCount

    averages.foreach { case (k, v) =>
      if (data.size > 0) {
        averages.update(k, v / data.size)
      } else {
        averages.update(k, 0.0)
      }
    }

    CwStatistic(averages, passCount, failCount)
  }

  def computeStatistic(data: Iterable[CourseworkDetailsAPI]): CwStatistic = {
    var averages = LinkedHashMap[String, Double]()
    val size = data.size
    var passCount = 0;
    var gradeFrequency = LinkedHashMap[String,Int]()

    data.foreach { d =>
      val courseworksMap = d.courseworks

      if (gradeFrequency.contains(d.grade.name)) {
        gradeFrequency.update(d.grade.name, gradeFrequency.get(d.grade.name).get + 1)
      } else {
        gradeFrequency += (d.grade.name -> 1)
      }

      d.courseworks.foreach { case (key, value) =>
        if (averages.contains(key)) {
          averages.update(key, averages.get(key).get + value)
        } else {
          averages += (key -> value)
        }

      }

      if (d.status == "Pass") { passCount += 1 }
    }


    val total = data.map(_.total).reduceOption(_ + _).getOrElse(0.0)
    averages += ("Total" -> total)
    val failCount = size - passCount

    averages.foreach { case (k, v) =>
      if (data.size > 0) {
        averages.update(k, v / data.size)
      } else {
        averages.update(k, 0.0)
      }
    }

    CwStatistic(averages, passCount, failCount, gradeFrequency)
  }

 }
