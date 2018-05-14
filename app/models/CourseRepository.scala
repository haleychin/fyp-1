package models

import javax.inject.{ Inject, Singleton }
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import scala.util.{Try}

import java.sql.Timestamp
import slick.driver.PostgresDriver.api._

import scala.concurrent.{ Future, ExecutionContext }

@Singleton
class CourseRepository @Inject() (dbConfigProvider: DatabaseConfigProvider, userRepository: UserRepository)(implicit ec: ExecutionContext) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._    // Bring db in scope
  import profile.api._ // Slick DSL

  private val users = TableQuery[userRepository.UserTable]
  // Define table
  private class CourseTable(tag: Tag) extends Table[Course](tag, "courses") {

    // Define columns
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def userId = column[Long]("user_id")
    def title = column[String]("title")
    def createdAt = column[Timestamp]("created_at", O.SqlType("timestamp default now()"))
    def updatedAt = column[Timestamp]("updated_at", O.SqlType("timestamp default now()"))
    def user = foreignKey("user_fk", userId, users)(_.id, onUpdate=ForeignKeyAction.Restrict, onDelete=ForeignKeyAction.Cascade)

    // Default Projection
    def * = (id, userId, title, createdAt, updatedAt) <> (Course.tupled, Course.unapply)
  }

  private val courses = TableQuery[CourseTable]

  // Print SQL command to create table
  // courses.schema.create.statements.foreach(println)

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
  def create(title: String): Future[Try[Course]] = {
    val seq = (
    (courses.map(u => u.title)
      returning courses.map(c => (c.id, c.userId, c.createdAt, c.updatedAt))
      into ((courseTitle, c) => Course(c._1, c._2, courseTitle, c._3, c._4))
      ) += (title)
    )
    db.run(seq.asTry)
  }

  def update(id: Long, title: String): Future[Int] = {
    val course = courses.filter(_.id === id)
    val action = course.map(u => (u.title)).update(title)
    db.run(action)
  }

  def delete(id: Long): Future[Int] = {
    val action = courses.filter(_.id === id).delete
    db.run(action)
  }
}


