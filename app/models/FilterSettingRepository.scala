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
  passingMark: Int, passingMarkPoint: Double,
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
    def passingMarkPoint = column[Double]("passing_mark_point")

    def courseworkMark = column[Int]("coursework_mark")
    def courseworkMarkPoint = column[Double]("coursework_mark_point")

    def courses = foreignKey("fk_courses", courseId, courseRepo.courses)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)
    def * = (id, courseId,
      attendanceRate, attendanceRatePoint,
      consecutiveMissed, consecutiveMissedPoint,
      absentCount, absentCountPoint,
      passingMark, passingMarkPoint,
      courseworkMark, courseworkMarkPoint) <> (FilterSetting.tupled, FilterSetting.unapply)
  }
  val settings = TableQuery[FilterSettingTable]

  def create(courseId: Long,
    attendanceRate: Int, attendanceRatePoint: Double,
    consecutiveMissed: Int, consecutiveMissedPoint: Double,
    absentCount: Int, absentCountPoint: Double,
    passingMark: Int, passingMarkPoint: Double,
    courseworkMark: Int, courseworkMarkPoint: Double): Future[FilterSetting] = {
    val seq = (
    (settings.map(u => (u.courseId,
      u.attendanceRate, u.attendanceRatePoint,
      u.consecutiveMissed, u.consecutiveMissedPoint,
      u.absentCount, u.absentCountPoint,
      u.passingMark, u.passingMarkPoint,
      u.courseworkMark, u.courseworkMarkPoint))
      returning settings.map(d => d.id)
      into ((metric, d) => FilterSetting(
        d, metric._1, metric._2, metric._3, metric._4, metric._5,
        metric._6, metric._7, metric._8, metric._9, metric._10, metric._11)
      )
      ) += (courseId,
        attendanceRate, attendanceRatePoint,
        consecutiveMissed, consecutiveMissedPoint,
        absentCount, absentCountPoint,
        passingMark, passingMarkPoint,
        courseworkMark, courseworkMarkPoint)
    )
    db.run(seq)
  }

}

