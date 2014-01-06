package models

import play.api.db.DB
import play.api.Play.current
import anorm._
import anorm.SqlParser._

case class User(name: String, email: String)

object User {

  val user = {
    get[String]("name") ~ get[String]("email") map {
      case name~email => User(name, email)
    }
  }

  def getAuthenticatedUser(email: String, password: String) : Option[User]  = DB.withConnection { implicit c =>
    SQL("select name,email from user where email = {email} and hash = SHA2({password}, 512);").on(
      'email -> email,
      'password -> password
    ).as(user.singleOpt)
  }

  def getUserByEmail(email: String) : Option[User]  = DB.withConnection { implicit c =>
    SQL("select name,email from user where email = {email}").on(
      'email -> email
    ).as(user.singleOpt)
  }

}
