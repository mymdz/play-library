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

  def all(implicit userId: Long) : Future[Seq[Book]] = db.run(query.filter(_.user_id === userId).sortBy(_.id.desc).result)

  def insert(title: String, author: String, isbn: String, userId: Long): Future[Book] = db.run {
    (query.map(b => (b.title, b.author, b.isbn, b.user_id))
      returning query.map(_.id)
      into ((inserted, id) => models.Book(id, inserted._1, inserted._2, inserted._3))
    ) += (title, author, isbn, userId)
  }

  def find(id: Long)(implicit userId: Long): Future[Seq[Book]] = db.run {
    query.filter(c => c.id === id && c.user_id === userId).result
  }

  def update(id: Long, title: String, author: String, isbn: String)(implicit userId: Long) = db.run {
    query.filter(_.id === id).filter(_.user_id === userId).map(b => (b.title, b.author, b.isbn)).update(title, author, isbn)
  }

  def delete(id: Long)(implicit userId: Long) = db.run (query.filter(_.id === id).filter(_.user_id === userId).delete)

  private class BooksTableDef(tag: Tag) extends Table[Book](tag, "books") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def title = column[String]("title")
    def author = column[String]("author")
    def isbn = column[String]("isbn")
    def user_id = column[Long]("user_id")

    override def * = (id, title, author, isbn) <> (Book.tupled, Book.unapply)
  }
}