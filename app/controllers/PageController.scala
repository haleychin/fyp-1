package controllers

import javax.inject._
import play.api.mvc._

class PageController @Inject()(cc: ControllerComponents)
 extends AbstractController(cc) with play.api.i18n.I18nSupport {

  /**
   * The index action.
   */
  def index = Action { implicit request =>
    Ok(views.html.index())
  }

 }
