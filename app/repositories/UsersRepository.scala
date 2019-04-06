package repositories

import java.util.concurrent.TimeUnit

import com.google.inject.Inject
import javax.inject.Singleton
import play.api.db.slick.DatabaseConfigProvider
import slick.jdbc.JdbcProfile
import models.User

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext}

@Singleton
class UsersRepository @Inject() (databaseConfigProvider: DatabaseConfigProvider)(implicit ec: ExecutionContext) {
  private val dbConfig = databaseConfigProvider.get[JdbcProfile]

  import dbConfig._
  import profile.api._

  private val query = TableQuery[UsersTableDef]

  def checkCredentials(login: String, hashedPass: String): Boolean = {
    val promise = db.run {
      val q = for {
        u <- query if u.login === login && u.password === hashedPass
      } yield u.id

      q.exists.result
    }

    Await.result(promise, Duration(3, TimeUnit.SECONDS))
  }

  private class UsersTableDef(tag: Tag) extends Table[User](tag, "users") {
    def id = column[Long]("id", O.PrimaryKey, O.AutoInc)
    def login = column[String]("login")
    def password = column[String]("password")

    override def * = (id, login, password) <> (User.tupled, User.unapply)
  }
}
