package models

import anorm._
import anorm.SqlParser._
import org.joda.time.LocalDate
import play.api.db._
import play.api.Play.current
import java.util.Date


case class Entry(date: LocalDate, text: String)

object Entry {

  val entry = {
    get[Date]("entryDate") ~
      get[String]("entryText") map {
      case entryDate~entryText => Entry(new LocalDate(entryDate), entryText)
    }
  }


  def all(): List[Entry] = DB.withConnection { implicit c =>
    SQL("select * from entry order by entryDate desc, id desc").as(entry *)
  }

  def create(date: LocalDate, text: String) {
    DB.withConnection { implicit c =>
      SQL("insert into entry (entryDate, entryText) values ({date}, {text})").on(
        'date -> date.toDate,
        'text -> text
      ).executeUpdate()
    }
  }

}
