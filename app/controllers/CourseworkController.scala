package controllers

import javax.inject._
import play.api.mvc._
import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

// For Form
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._

// For Files
import java.nio.file.Paths
import java.io.File

// Model
import models._
import utils._

case class CourseworkFormData(courseworks: List[String])

class CourseworkController @Inject()(
  repo: CourseworkRepository,
  csRepo: CourseStudentRepository,
  cRepo: CourseRepository,
  fsRepo: FilterSettingRepository,
  bbParser: BlackboardParser,
  authenticatedAction: AuthenticatedAction,
  cc: MessagesControllerComponents)
(implicit ec: ExecutionContext) extends
AbstractController(cc) with play.api.i18n.I18nSupport {

  val form = Form {
    mapping(
      "Courseworks" -> list(text)
    )(CourseworkFormData.apply)(CourseworkFormData.unapply)
  }

  def getCourseworkDetail(id: Long): Future[CourseCwAPI] = {
    val courseFuture      = cRepo.get(id)
    val studentFuture     = csRepo.getStudents(id)
    val courseworksFuture = repo.getCourseworks(id)
    val filterFuture      = fsRepo.get(id)

    val results = for {
      course      <- courseFuture
      students    <- studentFuture
      courseworks <- courseworksFuture
      filter      <- filterFuture
    } yield (course, students, courseworks, filter)


    results.map { r =>
      CourseCwAPI(r._1, r._2, r._3, r._4.get.courseworkThreshold)
    }
  }

  def index(id: Long) = Action.async { implicit request =>
    getCourseworkDetail(id).map { courseworkApi =>
      courseworkApi.course match {
        case Some(c) =>
          Ok(views.html.coursework.index(c,
            courseworkApi.students,
            courseworkApi.courseworks,
            courseworkApi.threshold
            ))
        case None => Redirect(routes.CourseController.index).flashing("error" -> "Course not found.")
      }
    }
  }

  def newImport(courseId: Long) = authenticatedAction { implicit request =>
    Ok(views.html.coursework.newImport(courseId))
  }

  def save(courseId: Long) = authenticatedAction(parse.multipartFormData) { implicit request =>
    request.body.file("file").map { file =>
      val filename = Paths.get(file.filename).getFileName
      if (s"$filename" != "") {
        file.ref.moveTo(Paths.get(s"$filename"), replace = true)
        bbParser.parse(filename.toString())
        Redirect(routes.CourseworkController.selection(courseId)).flashing(
        "success" -> s"Import courseworks successfully")
      } else {
        Redirect(routes.CourseController.showCourse(courseId)).flashing(
          "error" -> "Missing courseworks file")
      }
    }.getOrElse {
      Redirect(routes.PageController.index).flashing(
        "error" -> "Missing courseworks file")
    }
  }

  def selection(courseId: Long) = authenticatedAction { implicit request =>
    val courseworks = bbParser.header.map { case (value, _) =>
      (value, value)
    }.toSeq
    Ok(views.html.coursework.selection(courseId, form, courseworks))
  }

  def saveSelection(courseId: Long) = authenticatedAction { implicit request =>
    form.bindFromRequest.fold(
      errorForm => {
        val courseworks = bbParser.header.map { case (value, _) =>
          (value, value)
        }.toSeq
        Ok(views.html.coursework.selection(courseId, errorForm, courseworks))
      },
      form => {
        bbParser.getCourseworks(form.courseworks)
        bbParser.saveToDb(courseId, repo)
        Redirect(routes.CourseController.showCourse(courseId)).flashing("success" -> "Successfully import courseworks marks.")
      }
    )
  }

  def delete(courseId: Long) = authenticatedAction.async { implicit request =>
    repo.delete(courseId).map { _ =>
      Redirect(routes.CourseworkController.index(courseId)).flashing(
        "success" -> "Successfully remove all courseworks")
    }
  }

}

