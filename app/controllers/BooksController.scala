package controllers

import controllers.Auth._
import javax.inject._
import play.api.mvc._
import repositories.BooksRepository
import play.api.data.Form
import play.api.data.Forms._

import scala.collection.immutable
import scala.collection.immutable.Map
import scala.concurrent.Await
import scala.concurrent.duration.Duration

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class BooksController @Inject()(cc: ControllerComponents, booksRepository: BooksRepository)
  extends AbstractController(cc) with play.api.i18n.I18nSupport {

  def index() = Action { implicit request: Request[AnyContent] =>
    if (request.session.get("login").isDefined) {
      val books = Await.result(booksRepository.all, Duration.Inf)
      Ok(views.html.listBooks(books))
    } else {
      Redirect(routes.AuthController.auth())
    }
  }

  def create() = Action { implicit request: Request[AnyContent] =>
    if (request.session.get("login").isDefined) {
      val form = BooksController.bookForm
      Ok(views.html.editBook(form, "Добавление книги", routes.BooksController.postCreate()))
    } else {
      Redirect(routes.AuthController.auth())
    }
  }


  def edit(id: Long) = Action { implicit request: Request[AnyContent] =>
    if (request.session.get("login").isDefined) {
      val book = Await.result(booksRepository.find(id), Duration.Inf).headOption

      if (book.isDefined) {
        val bookObj = book.get

        val form = BooksController.bookForm.bind(immutable.Map(
          "title" -> bookObj.title,
          "author" -> bookObj.author,
          "isbn" -> bookObj.isbn,
        ))

        Ok(views.html.editBook(form, "Редактирование книги #"+id, routes.BooksController.postUpdate(id)))
      } else {
        NotFound("Not found mother fucker")
      }

    } else {
      Redirect(routes.AuthController.auth())
    }
  }

  def postCreate() = Action { implicit request: Request[AnyContent] =>
    if (request.session.get("login").isDefined) {
      val err = { formWithErrors: Form[BooksController.BookFormData] =>
        BadRequest(views.html.editBook(formWithErrors, "Добавление книги", routes.BooksController.postCreate()))
      }

      val ok = { data: BooksController.BookFormData =>
        Await.result(booksRepository.insert(data.title, data.author, data.isbn), Duration.Inf)
        Redirect(routes.BooksController.index())
      }

      val data = BooksController.bookForm.bindFromRequest
      data.fold(err, ok)
    } else {
      Redirect(routes.AuthController.auth())
    }
  }


  def postUpdate(id: Long) = Action { implicit request: Request[AnyContent] =>
    if (request.session.get("login").isDefined) {
      val err = { formWithErrors: Form[BooksController.BookFormData] =>
        BadRequest(views.html.editBook(formWithErrors, "Редактирование книги #"+id, routes.BooksController.postUpdate(id)))
      }

      val ok = { data: BooksController.BookFormData =>
        Await.result(booksRepository.update(id, data.title, data.author, data.isbn), Duration.Inf)
        Redirect(routes.BooksController.index())
      }

      val data = BooksController.bookForm.bindFromRequest
      data.fold(err, ok)
    } else {
      Redirect(routes.AuthController.auth())
    }
  }

  def delete(id: Long) = Action { implicit request: Request[AnyContent] =>
    if (request.session.get("login").isDefined) {
      booksRepository.delete(id)
      Redirect(routes.BooksController.index())
    } else {
      Redirect(routes.AuthController.auth())
    }
  }


}

object BooksController {
  case class BookFormData(title: String, author: String, isbn: String)

  val bookForm = Form(
    mapping(
      "title" -> nonEmptyText,
      "author" -> nonEmptyText,
      "isbn" -> nonEmptyText,
    )(BookFormData.apply)(BookFormData.unapply)
  )
}