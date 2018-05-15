package models

import javax.inject.{ Inject, Singleton }
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import scala.util.{Try}

import java.sql.{Date, Timestamp}
import slick.driver.PostgresDriver.api._

import scala.concurrent.{ Future, ExecutionContext }

case class Student(id: Long, name: String, email: String,
  studentId: String, icOrPassport: String, nationality: String,
  contactNumber: String, birthDate: Date, programme: String,
  intake: String, semester: Int, createdAt: Timestamp,
  updatedAt: Timestamp)

@Singleton
class StudentRepository @Inject() (dbConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  val dbConfig = dbConfigProvider.get[JdbcProfile]

  import dbConfig._    // Bring db in scope
  import profile.api._ // Slick DSL

  // Define table
  class StudentTable(tag: Tag) extends Table[Student](tag, "students") {

    // Define columns
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def name = column[String]("name")
    def email = column[String]("email", O.Unique)
    def studentId = column[String]("student_id", O.Unique)
    def icOrPassport = column[String]("ic_or_passport")
    def nationality = column[String]("nationality")
    def contactNumber = column[String]("contact_number")
    def birthDate = column[Date]("birth_date")
    def programme = column[String]("programme")
    def intake    = column[String]("intake")
    def semester  = column[Int]("semester")
    def createdAt = column[Timestamp]("created_at", O.SqlType("timestamp default now()"))
    def updatedAt = column[Timestamp]("updated_at", O.SqlType("timestamp default now()"))

    // Default Projection
    def * = (id, name, email, studentId, icOrPassport, nationality, contactNumber,
      birthDate, programme, intake, semester, createdAt, updatedAt) <> (Student.tupled, Student.unapply)
  }

  private val students = TableQuery[StudentTable]

  // Print SQL command to create table
  students.schema.create.statements.foreach(println)

  // =================
  // Define CRUD here.
  // =================

  // List all students.
  // def list(): Future[Seq[Student]] = db.run {
  //   students.result
  // }

  // def get(id: Long): Future[Option[Student]] = db.run {
  //   students.filter(_.id === id).result.headOption
  // }

  // // Create Student
  // def create(name: String, email: String): Future[Try[Student]] = {
  //   val seq = (
  //   (students.map(u => (u.name, u.email, u.passwordHash))
  //     returning students.map(u => (u.id, u.createdAt, u.updatedAt))
  //     into ((form, student) => Student(student._1, form._1, form._2, form._3, student._2, student._3))
  //     ) += (name, email, password)
  //   )
  //   db.run(seq.asTry)
  // }

  // def update(id: Long, name: String, email: String): Future[Int] = {
  //   val student = students.filter(_.id === id)
  //   val action = student.map(u => (u.name, u.email)).update(name, email)
  //   db.run(action)
  // }

  // def delete(id: Long): Future[Int] = {
  //   val action = students.filter(_.id === id).delete
  //   db.run(action)
  // }
}


