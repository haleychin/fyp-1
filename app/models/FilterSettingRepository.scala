package models

import javax.inject.{Inject,Singleton}
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import slick.driver.PostgresDriver.api._

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

case class FilterSetting(id: Long, courseId: Long,
  attendanceRate: Int, attendanceRatePoint: Double,
  consecutiveMissed: Int, consecutiveMissedPoint: Double,
  absentCount: Int, absentCountPoint: Double,
  passingMark: Int, overviewThreshold: Double,
  attendanceThreshold: Double, courseworkThreshold: Double,
  courseworkMark: Int, courseworkMarkPoint: Double)

@Singleton
class FilterSettingRepository @Inject() (
  dbConfigProvider: DatabaseConfigProvider,
  val courseRepo: CourseRepository
)(implicit ec: ExecutionContext) {
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._    // Bring db in scope
  import profile.api._ // Slick DSL

  // =============
  // FilterSetting
  // =============
  class FilterSettingTable(tag: Tag) extends Table[FilterSetting](tag, "filters") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def courseId = column[Long]("course_id")

    def attendanceRate = column[Int]("attendance_rate")
    def attendanceRatePoint = column[Double]("attendance_rate_point")

    def consecutiveMissed = column[Int]("consecutive_missed")
    def consecutiveMissedPoint = column[Double]("consecutive_missed_point")

    def absentCount = column[Int]("absent_count")
    def absentCountPoint = column[Double]("absent_count_point")

    def passingMark = column[Int]("passing_mark")
    def overviewThreshold = column[Double]("overview_threshold")
    def attendanceThreshold = column[Double]("attendance_threshold")
    def courseworkThreshold = column[Double]("coursework_threshold")

    def courseworkMark = column[Int]("coursework_mark")
    def courseworkMarkPoint = column[Double]("coursework_mark_point")

    def courses = foreignKey("fk_courses", courseId, courseRepo.courses)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
    def * = (id, courseId,
      attendanceRate, attendanceRatePoint,
      consecutiveMissed, consecutiveMissedPoint,
      absentCount, absentCountPoint,
      passingMark, overviewThreshold,
      attendanceThreshold, courseworkThreshold,
      courseworkMark, courseworkMarkPoint) <> (FilterSetting.tupled, FilterSetting.unapply)
  }
  val settings = TableQuery[FilterSettingTable]
  // settings.schema.create.statements.foreach(println)

  def create(courseId: Long,
    attendanceRate: Int, attendanceRatePoint: Double,
    consecutiveMissed: Int, consecutiveMissedPoint: Double,
    absentCount: Int, absentCountPoint: Double,
    passingMark: Int, overviewThreshold: Double,
    attendanceThreshold: Double, courseworkThreshold: Double,
    courseworkMark: Int, courseworkMarkPoint: Double): Future[FilterSetting] = {
    val seq = (
    (settings.map(u => (u.courseId,
      u.attendanceRate, u.attendanceRatePoint,
      u.consecutiveMissed, u.consecutiveMissedPoint,
      u.absentCount, u.absentCountPoint,
      u.passingMark, u.overviewThreshold,
      u.attendanceThreshold, u.courseworkThreshold,
      u.courseworkMark, u.courseworkMarkPoint))
      returning settings.map(d => d.id)
      into ((metric, d) => FilterSetting(
        d, metric._1, metric._2, metric._3, metric._4, metric._5,
        metric._6, metric._7, metric._8, metric._9, metric._10, metric._11,
        metric._12, metric._13)
      )
      ) += (courseId,
        attendanceRate, attendanceRatePoint,
        consecutiveMissed, consecutiveMissedPoint,
        absentCount, absentCountPoint,
        passingMark, overviewThreshold,
        attendanceThreshold, courseworkThreshold,
        courseworkMark, courseworkMarkPoint)
    )
    db.run(seq)
  }

  def update(courseId: Long,
    attendanceRate: Int, attendanceRatePoint: Double,
    consecutiveMissed: Int, consecutiveMissedPoint: Double,
    absentCount: Int, absentCountPoint: Double,
    passingMark: Int, overviewThreshold: Double,
    attendanceThreshold: Double, courseworkThreshold: Double,
    courseworkMark: Int, courseworkMarkPoint: Double): Future[Int] = {
      val setting = settings.filter(_.courseId === courseId)
      val action = setting.map(u => (
      u.attendanceRate, u.attendanceRatePoint,
      u.consecutiveMissed, u.consecutiveMissedPoint,
      u.absentCount, u.absentCountPoint,
      u.passingMark, u.overviewThreshold,
      u.attendanceThreshold, u.courseworkThreshold,
      u.courseworkMark, u.courseworkMarkPoint)).update(
        attendanceRate, attendanceRatePoint,
        consecutiveMissed, consecutiveMissedPoint,
        absentCount, absentCountPoint,
        passingMark, overviewThreshold,
        attendanceThreshold, courseworkThreshold,
        courseworkMark, courseworkMarkPoint)
      db.run(action)
  }

  def get(courseId: Long): Future[Option[FilterSetting]] = db.run {
    settings.filter(_.courseId === courseId).result.headOption
  }

}

