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

  val addBookTitle = "Добавление книги"

  def editBookTitle(id: Long) = "Редактирование книги #"+id

  def authorizedAction(result: Request[AnyContent] => Long => Result): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    val user = request.session.get("user_id")
    if (user.isDefined) {
      result(request)(user.get.toLong)
    } else {
      Redirect(routes.AuthController.auth())
    }
  }

  def index() = authorizedAction { implicit request: Request[AnyContent] => implicit userId: Long =>
    val books = Await.result(booksRepository.all, Duration.Inf)
    Ok(views.html.listBooks(books))
  }

  def create() = authorizedAction { implicit request: Request[AnyContent] => implicit userId: Long =>
    val form = BooksController.bookForm
    Ok(views.html.editBook(form, addBookTitle, routes.BooksController.postCreate()))
  }


  def edit(id: Long) = authorizedAction { implicit request: Request[AnyContent] => implicit userId: Long =>
    val book = Await.result(booksRepository.find(id), Duration.Inf).headOption

    if (book.isDefined) {
      val bookObj = book.get

      val form = BooksController.bookForm.bind(immutable.Map(
        "title" -> bookObj.title,
        "author" -> bookObj.author,
        "isbn" -> bookObj.isbn,
      ))

      Ok(views.html.editBook(form, editBookTitle(id), routes.BooksController.postUpdate(id)))
    } else {
      NotFound("Книга не найдена")
    }
  }

  def postCreate() = authorizedAction { implicit request: Request[AnyContent] => implicit userId: Long =>
    val err = { formWithErrors: Form[BooksController.BookFormData] =>
      BadRequest(views.html.editBook(formWithErrors, addBookTitle, routes.BooksController.postCreate()))
    }

    val ok = { data: BooksController.BookFormData =>
      Await.result(booksRepository.insert(data.title, data.author, data.isbn, userId), Duration.Inf)
      Redirect(routes.BooksController.index())
    }

    val data = BooksController.bookForm.bindFromRequest
    data.fold(err, ok)
  }


  def postUpdate(id: Long) = authorizedAction { implicit request: Request[AnyContent] => implicit userId: Long =>
    val err = { formWithErrors: Form[BooksController.BookFormData] =>
      BadRequest(views.html.editBook(formWithErrors, editBookTitle(id), routes.BooksController.postUpdate(id)))
    }

    val ok = { data: BooksController.BookFormData =>
      Await.result(booksRepository.update(id, data.title, data.author, data.isbn), Duration.Inf)
      Redirect(routes.BooksController.index())
    }

    val data = BooksController.bookForm.bindFromRequest
    data.fold(err, ok)
  }

  def delete(id: Long) = authorizedAction { implicit request: Request[AnyContent] => implicit userId: Long =>
    booksRepository.delete(id)
    Redirect(routes.BooksController.index())
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