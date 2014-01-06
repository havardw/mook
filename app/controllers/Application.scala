package controllers

import play.api.mvc._
import play.api.data._
import play.api.data.Forms._
import models.User

object Application extends Controller {

  def index = Action {
    Redirect(routes.Entries.entries)
  }

  val loginForm = Form(
    tuple(
      "email" -> email,
      "password" -> text
    ) verifying ("Invalid email or password", result => result match {
      case (email, password) => User.getAuthenticatedUser(email, password).isDefined
    })
  )

  def login = Action {
    Ok(views.html.login(loginForm))
  }

  def handleLogin = Action { implicit request =>
    val u = User.getAuthenticatedUser("havardw@wigtil.net", "test")
    loginForm.bindFromRequest.fold(
      formWithErrors => BadRequest(views.html.login(formWithErrors)),
      user => Redirect(routes.Entries.entries).withSession("username" -> user._1) /* TODO Get use name instead of e-mail. */
    )
  }
}
