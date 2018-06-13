package models

import javax.inject.{ Inject, Singleton }
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import scala.util.{Try}

import java.sql.{Timestamp,Date}
import slick.driver.PostgresDriver.api._

import scala.concurrent.{ Future, ExecutionContext }

case class Course(id: Long, userId: Long, title: String, code: String,
  startDate: Date, createdAt: Timestamp, updateAt: Timestamp)

@Singleton
class CourseRepository @Inject() (
  dbConfigProvider: DatabaseConfigProvider,
  val userRepository: UserRepository
)
(implicit ec: ExecutionContext) {
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._    // Bring db in scope
  import profile.api._ // Slick DSL

  // Define table
  class CourseTable(tag: Tag) extends Table[Course](tag, "courses") {

    // Define columns
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def userId = column[Long]("user_id")
    def title = column[String]("title")
    def code = column[String]("code")
    def startDate = column[Date]("start_date")
    def createdAt = column[Timestamp]("created_at", O.SqlType("timestamp default now()"))
    def updatedAt = column[Timestamp]("updated_at", O.SqlType("timestamp default now()"))
    def user = foreignKey("user_fk", userId, userRepository.users)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

    // Default Projection
    def * = (id, userId, title, code, startDate, createdAt, updatedAt) <> (Course.tupled, Course.unapply)
  }

  val courses = TableQuery[CourseTable]

  // =================
  // Define CRUD here.
  // =================
  def list(): Future[Seq[Course]] = db.run {
    courses.result
  }

  def get(id: Long): Future[Option[Course]] = db.run {
    courses.filter(_.id === id).result.headOption
  }

  // Create Course
  def create(title: String, code: String, startDate: Date, userId: Long): Future[Course] = {
    val seq = (
    (courses.map(u => (u.userId, u.title, u.code, u.startDate))
      returning courses.map(c => (c.id, c.createdAt, c.updatedAt))
      into ((course, c) => Course(c._1, course._1, course._2, course._3, course._4, c._2, c._3))
      ) += (userId, title, code, startDate)
    )
    db.run(seq)
  }

  def update(id: Long, title: String, code: String, startDate: Date): Future[Int] = {
    val course = courses.filter(_.id === id)
    val action = course.map(u => (u.title, u.code, u.startDate)).update(title, code, startDate)
    db.run(action)
  }

  def delete(id: Long): Future[Int] = {
    val action = courses.filter(_.id === id).delete
    db.run(action)
  }
}


