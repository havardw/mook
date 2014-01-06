package controllers

import play.api.data._
import play.api.data.Forms._
import play.api.mvc._
import models.{EntryForm, User, Entry}

object Entries extends Controller with Secured {

  val entryForm = Form(
    mapping(
      "date" -> jodaLocalDate("yyyy-MM-dd"),
      "text" -> nonEmptyText
    )(EntryForm.apply)(EntryForm.unapply)
  )

  def entries = IsAuthenticated { email => _ =>
    User.getUserByEmail(email).map { user =>
      Ok(views.html.index(Entry.all(), entryForm, user))
    }.getOrElse(Forbidden)
  }

  def newEntry = IsAuthenticated { email => implicit request =>
    entryForm.bindFromRequest.fold(
      errors => User.getUserByEmail(email).map { user =>
        BadRequest(views.html.index(Entry.all(), errors, user))
      }.getOrElse(Forbidden),
      entry => User.getUserByEmail(email).map { user =>
        Entry.create(user.name, entry.date, entry.text)
        Redirect(routes.Entries.entries)
      }.getOrElse(Forbidden)
    )
  }

}