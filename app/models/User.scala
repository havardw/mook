package models

import play.api.db.DB
import play.api.Play.current
import anorm._
import anorm.SqlParser._


object User {

  def getAuthenticatedUser(email: String, password: String) : Option[String]  = DB.withConnection { implicit c =>
    SQL("select name from user where email = '{email}' and hash = SHA2('{password}', 512);").on(
      'email -> email,
      'password -> password
    ).as(scalar[String].singleOpt)
  }

}
