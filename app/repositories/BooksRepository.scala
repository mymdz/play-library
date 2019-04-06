package repositories

import java.util.concurrent.TimeUnit

import com.google.inject.Inject
import javax.inject.Singleton
import models.Book
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

@Singleton
class BooksRepository @Inject()(databaseConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  private val dbConfig = databaseConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  private val query = TableQuery[BooksTableDef]

  def all() : Future[Seq[Book]] = db.run(query.sortBy(_.id.desc).result)

  def insert(title: String, author: String, isbn: String): Future[Book] = db.run {
    (query.map(b => (b.title, b.author, b.isbn))
      returning query.map(_.id)
      into ((inserted, id) => models.Book(id, inserted._1, inserted._2, inserted._3))
    ) += (title, author, isbn)
  }

  def find(id: Long): Future[Seq[Book]] = db.run {
    query.filter(_.id === id).result
  }

  def update(id: Long, title: String, author: String, isbn: String) = db.run {
    query.filter(_.id === id).map(b => (b.title, b.author, b.isbn)).update(title, author, isbn)
  }

  def delete(id: Long) = db.run (query.filter(_.id === id).delete)

  private class BooksTableDef(tag: Tag) extends Table[Book](tag, "books") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def title = column[String]("title")
    def author = column[String]("author")
    def isbn = column[String]("isbn")

    override def * = (id, title, author, isbn) <> (Book.tupled, Book.unapply)
  }
}