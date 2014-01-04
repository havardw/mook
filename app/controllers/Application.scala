package controllers

import play.api._
import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import models.Entry
import java.util.Date

object Application extends Controller {

  val entryForm = Form(
    mapping(
      "date" -> jodaLocalDate("yyyy-MM-dd"),
      "text" -> nonEmptyText
    )(Entry.apply)(Entry.unapply)
  )

  def index = Action {
    Redirect(routes.Application.entries)
  }

  def entries = Action {
    Ok(views.html.index(Entry.all(), entryForm))
  }

  def newEntry = Action { implicit request =>
    entryForm.bindFromRequest.fold(
      errors => BadRequest(views.html.index(Entry.all(), errors)),
      entry => {
        Entry.create(entry.date, entry.text)
        Redirect(routes.Application.entries)
      }
    )
  }

}