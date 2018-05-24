
package controllers

import javax.inject._
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Success, Failure}

// For Form
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

// Model
import models._

import java.sql.Date

case class StudentData(name: String, email: String,
  studentId: String, icOrPassport: String, nationality: String,
  contactNumber: String, birthDate: Date, programme: String,
  intake: String, semester: Int)
case class StudentAPI(student: Option[Student], courses: Seq[Course])

class StudentController @Inject()(
  repo: StudentRepository,
  csRepo: CourseStudentRepository,
  authenticatedAction: AuthenticatedAction,
  cc: MessagesControllerComponents)
(implicit ec: ExecutionContext) extends
AbstractController(cc) with play.api.i18n.I18nSupport {

  val studentForm = Form {
    mapping(
      "name" -> nonEmptyText,
      "email" -> email,
      "studentId" -> nonEmptyText,
      "icOrPassport" -> nonEmptyText,
      "nationality" -> nonEmptyText,
      "contactNumber" -> nonEmptyText,
      "birthDate" -> sqlDate,
      "programme" -> nonEmptyText,
      "intake" -> nonEmptyText,
      "semester" -> number,
    )(StudentData.apply)(StudentData.unapply)
  }

  def index = authenticatedAction.async { implicit request =>
    repo.list().map { students =>
      Ok(views.html.student.index(students))
    }
  }

  def newStudent = authenticatedAction { implicit request =>
    Ok(views.html.student.newStudent(studentForm))
  }

  def getStudentDetail(id: Long): Future[StudentAPI] = {
    val studentFuture = repo.get(id)
    val coursesFuture = csRepo.getCourses(id)

    val result = for {
      student <- studentFuture
      courses <- coursesFuture
    } yield (student, courses)

    result.map { result =>
      StudentAPI(result._1, result._2)
    }
  }

  def show(id: Long) = authenticatedAction.async { implicit request =>
    getStudentDetail(id).map { studentApi =>
      studentApi.student match {
        case Some(s) =>
          Ok(views.html.student.show(s, studentApi.courses))
        case None => Ok(views.html.index())
      }
    }
  }

  def create = authenticatedAction.async { implicit request =>
    studentForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(Ok(views.html.student.newStudent(errorForm)))
      },
      student => {
        repo.create(
          student.name,
          student.email,
          student.studentId,
          student.icOrPassport,
          student.nationality,
          student.contactNumber,
          student.birthDate,
          student.programme,
          student.intake,
          student.semester
        ).map { result =>
          result match {
            case Failure(t) =>
              val errorForm = studentForm
                .withError("email", "already been taken")
                .withError("studentId", "already been taken")
              Ok(views.html.student.newStudent(errorForm))
            case Success(_) =>
              Redirect(routes.StudentController.index).flashing("success" -> "Student has been successfully created.")
          }
        }
      }
    )
  }

  def edit(id: Long) = authenticatedAction.async { implicit request =>
    repo.get(id).map { result =>
      result match {
        case Some(student) =>
          val filledForm = studentForm.fill(
            StudentData(
              student.name,
              student.email,
              student.studentId,
              student.icOrPassport,
              student.nationality,
              student.contactNumber,
              student.birthDate,
              student.programme,
              student.intake,
              student.semester
            )
          )
          Ok(views.html.student.edit(id, filledForm))
        case None => Ok(views.html.index())
      }
    }
  }

  def update(id: Long) = authenticatedAction.async { implicit request =>
    studentForm.bindFromRequest.fold(
      errorForm => {
        Future.successful(Ok(views.html.student.edit(id, errorForm)))
      },
      student => {
        repo.update(
          id,
          student.name,
          student.email,
          student.studentId,
          student.icOrPassport,
          student.nationality,
          student.contactNumber,
          student.birthDate,
          student.programme,
          student.intake,
          student.semester
        ).map { result =>
          if (result > 0) {
            Redirect(routes.StudentController.show(id)).flashing("success" -> "Student has been successfully updated.")
          } else {
            Redirect(routes.StudentController.edit(id))
          }
        }
      }
    )
  }

  def delete(id: Long) = authenticatedAction.async { implicit request =>
      repo.delete(id).map { _ =>
        Redirect(routes.StudentController.index())
      }
  }
}
