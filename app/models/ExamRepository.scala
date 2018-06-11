package models

import javax.inject.{ Inject, Singleton }
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import scala.util.{Success, Failure}

import java.sql.Timestamp
import slick.driver.PostgresDriver.api._

import scala.concurrent.{ExecutionContext, Future, Await}
import scala.concurrent.duration._

import scala.collection.mutable.{LinkedHashMap, ArrayBuffer}
import utils.{Utils, Stats, DescriptiveStatistic}


case class Statistic(
  averageMark: Double,
  averageWeightage: Double,
  passCount: Int,
  failCount: Int)

case class ExamAPI(
  examDetails: LinkedHashMap[Long,ExamDetailsAPI],
  total: Int,
  weightage: Int,
  statistic: Statistic,
  descStat: DescriptiveStatistic)

case class ExamDetailsAPI(
  student: Student,
  // (Total, Weightage, Pass/Fail)
  var exam: (Double, Double, String))

case class CExamAPI(
  examDetails: LinkedHashMap[Long,CExamDetailsAPI])

case class CExamDetailsAPI(
  course: Course,
  // (Total, Weightage, Pass/Fail)
  var exam: (Double, Double, String),
  fullMark: Int,
  fullWeightage: Int)

case class Exam(courseId: Long, studentId: Long,
  mark: Double, totalMark: Double,
  weightage: Double, totalWeightage: Double,
  createdAt: Timestamp, updateAt: Timestamp)

@Singleton
class ExamRepository @Inject() (
  dbConfigProvider: DatabaseConfigProvider,
  val cRepo: CourseRepository,
  val sRepo: StudentRepository
)(implicit ec: ExecutionContext) {
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._    // Bring db in scope
  import profile.api._ // Slick DSL

  // Define table
  class ExamTable(tag: Tag) extends Table[Exam](tag, "exams") {

    // Define columns
    def courseId = column[Long]("course_id")
    def studentId = column[Long]("student_id")
    def mark = column[Double]("mark")
    def totalMark = column[Double]("total_mark")
    def weightage = column[Double]("weightage")
    def totalWeightage = column[Double]("total_weightage")
    def createdAt = column[Timestamp]("created_at", O.SqlType("timestamp default now()"))
    def updatedAt = column[Timestamp]("updated_at", O.SqlType("timestamp default now()"))
    def pk = primaryKey("pk_exam", (courseId, studentId))

    def courses = foreignKey("e_fk_courses", courseId, cRepo.courses)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
    def students = foreignKey("e_fk_students", studentId, sRepo.students)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

    // Default Projection
    def * = (courseId, studentId, mark, totalMark, weightage, totalWeightage, createdAt, updatedAt) <> (Exam.tupled, Exam.unapply)
  }

  val exams = TableQuery[ExamTable]

  // studentId here refer to the Student Id for student instead
  // of the primary key of the Student record
  def create(courseId: Long, studentId: String,
    mark: Double, totalMark: Double,
    weightage: Double, totalWeightage: Double): Future[Option[Exam]] = {
      sRepo.getByStudentId(studentId).flatMap { s =>
        s match {
          case Some(student) =>
            val seq = (
              (exams.map(a => (
                a.courseId, a.studentId, a.mark, a.totalMark, a.weightage, a.totalWeightage))
            returning exams.map(a => (a.createdAt, a.updatedAt))
            into ((form, a) => Exam(
              form._1, form._2, form._3, form._4, form._5, form._6, a._1, a._2))
          ) += (courseId, student.id, mark, totalMark, weightage, totalWeightage)
        )
            val result = db.run(seq.asTry)
            result.map { r =>
              r match {
                case Success(a) =>
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

  def getExams(
    courseId: Long,
    programme: String = "%",
    intake: String = "%"): Future[ExamAPI] = {
    val query = for {
      e <- exams
      courses <- e.courses if courses.id === courseId
      students <- e.students if (students.programme like programme) &&   (students.intake like intake)
    } yield (students, e)

    val result = db.run(query.result)
    val studentMap = LinkedHashMap[Long, ExamDetailsAPI]()

    result.map { r =>
      r.foreach { case (student, e) =>
        val pass = Utils.calculatePass(e.weightage, e.totalWeightage)
        val data = (e.mark, e.weightage, pass)
        studentMap += (student.id -> ExamDetailsAPI(student, data))
      }

      val statistic = computeStatistic(studentMap.values)
      val total     = r.headOption.map(_._2.totalMark.toInt).getOrElse(0)
      val weightage = r.headOption.map(_._2.totalWeightage.toInt).getOrElse(0)
      val marks     = studentMap.values.map(_.exam._1).toSeq
      val descStat  = Stats.computeDescriptiveStatistic(marks)
      ExamAPI(studentMap, total, weightage, statistic, descStat)
    }
  }

  def getExamsMetrics(
    courseId: Long,
    programme: String = "%",
    intake: String ="%") {
    val query = for {
      e <- exams
      courses <- e.courses if courses.id === courseId
      students <- e.students if (students.programme like programme) &&   (students.intake like intake)
    } yield (students, e)
  }

  def getCoursesExam(studentId: Long): Future[CExamAPI] = {
    val query = for {
      e <- exams
      courses <- e.courses
      students <- e.students if students.id === studentId
    } yield (courses, e)

    val result     = db.run(query.result)
    val courseMap = LinkedHashMap[Long, CExamDetailsAPI]()

    result.map { r =>
      r.foreach { case (course, e) =>
        val pass = Utils.calculatePass(e.weightage, e.totalWeightage)
        val data = (e.mark, e.weightage, pass)
        courseMap += (course.id -> CExamDetailsAPI(course, data, e.totalMark.toInt, e.totalWeightage.toInt))
      }

      CExamAPI(courseMap)
    }
  }

  def computeStatistic(data: Iterable[ExamDetailsAPI]): Statistic = {
    val size = data.size
    var total = 0.0;
    var totalWeightage = 0.0;
    var passCount = 0;

    data.foreach { d  =>
      val exam = d.exam
      total += exam._1
      totalWeightage += exam._2

      if (exam._3 == "Pass") { passCount += 1 }
    }

    val average = if (size > 0) {
      total / size
    } else { 0.0 }
    val averageWeightage =  if (size > 0) {
      totalWeightage / size
    } else { 0.0 }
    val failCount = size - passCount

    Statistic(average, averageWeightage, passCount, failCount)
  }

}
