package models

import anorm._
import anorm.SqlParser._
import org.joda.time.LocalDate
import play.api.db._
import play.api.Play.current
import java.util.Date

case class EntryForm(date: LocalDate, text: String)

case class Entry(author: String, date: LocalDate, text: String)

object Entry {

  val entry = {
    get[String]("author") ~ get[Date]("entryDate") ~ get[String]("entryText") map {
      case author~entryDate~entryText => Entry(author, new LocalDate(entryDate), entryText)
    }
  }


  def all(): List[Entry] = DB.withConnection { implicit c =>
    SQL("select * from entry order by entryDate desc, id desc").as(entry *)
  }

  def create(author: String, date: LocalDate, text: String) {
    DB.withConnection { implicit c =>
      SQL("insert into entry (author, entryDate, entryText) values ({author}, {date}, {text})").on(
        'author -> author,
        'date -> date.toDate,
        'text -> text
      ).executeUpdate()
    }
  }

}
