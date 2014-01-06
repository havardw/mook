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

trait Secured {

  /**
   * Retrieve the connected user's name.
   */
  private def username(request: RequestHeader) = request.session.get("username")

  /**
   * Redirect to login if the user in not authorized.
   */
  private def onUnauthorized(request: RequestHeader) = Results.Redirect(routes.Application.login)

  /**
   * Action for authenticated users.
   */
  def IsAuthenticated(f: => String => Request[AnyContent] => Result) = Security.Authenticated(username, onUnauthorized) { user =>
    Action(request => f(user)(request))
  }
}