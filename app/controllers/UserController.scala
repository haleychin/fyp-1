package controllers

import javax.inject._
import play.api.mvc._

class UserController @Inject()(cc: ControllerComponents)
extends AbstractController(cc) {

  def index = Action { implicit request =>
    Ok(views.html.user.index())
  }
}

