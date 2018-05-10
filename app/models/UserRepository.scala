package models

import javax.inject.{ Inject, Singleton }
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import scala.util.{Try}

import java.sql.Timestamp
import slick.driver.PostgresDriver.api._

import scala.concurrent.{ Future, ExecutionContext }

@Singleton
class UserRepository @Inject() (dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  private val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._    // Bring db in scope
  import profile.api._ // Slick DSL

  // Define table
  private class UserTable(tag: Tag) extends Table[User](tag, "users") {

    // Define columns
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def email = column[String]("email", O.Unique)
    def passwordHash = column[String]("password_hash")
    def createdAt = column[Timestamp]("created_at", O.SqlType("timestamp default now()"))
    def updatedAt = column[Timestamp]("updated_at", O.SqlType("timestamp default now()"))

    // Default Projection
    def * = (id, name, email, passwordHash, createdAt, updatedAt) <> (User.tupled, User.unapply)
  }

  private val users = TableQuery[UserTable]

  // Print SQL command to create table
  // users.schema.create.statements.foreach(println)

  // =================
  // Define CRUD here.
  // =================

  // List all users.
  def list(): Future[Seq[User]] = db.run {
    users.result
  }

  def getByEmail(email: String): Future[Option[User]] = db.run {
    users.filter(_.email === email).result.headOption
  }

  def get(id: Long): Future[Option[User]] = db.run {
    users.filter(_.id === id).result.headOption
  }

  // Create User
  def create(name: String, email: String, password: String): Future[Try[User]] = {
    val seq = (
    (users.map(u => (u.name, u.email, u.passwordHash))
      returning users.map(u => (u.id, u.createdAt, u.updatedAt))
      into ((form, user) => User(user._1, form._1, form._2, form._3, user._2, user._3))
      ) += (name, email, password)
    )
    db.run(seq.asTry)
  }

  def update(id: Long, name: String, email: String): Future[Int] = {
    val user = users.filter(_.id === id)
    val action = user.map(u => (u.name, u.email)).update(name, email)
    db.run(action)
  }
}


