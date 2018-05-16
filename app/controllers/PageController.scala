package controllers

import javax.inject._
import play.api.mvc._

import models._

class PageController @Inject()(
  cc: ControllerComponents,
  courseStudent: CourseStudentRepository
)
 extends AbstractController(cc) with play.api.i18n.I18nSupport {

  /**
   * The index action.
   */
  def index = Action { implicit request =>
    Ok(views.html.index())
  }

 }
