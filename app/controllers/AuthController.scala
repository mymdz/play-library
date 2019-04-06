package controllers

import java.security.MessageDigest

import controllers.Auth._
import javax.inject._
import play.api.data.Form
import play.api.mvc._
import play.filters.csrf.CSRF
import play.api.data.Forms._
import repositories.UsersRepository

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class AuthController @Inject()(cc: ControllerComponents, usersRepository: UsersRepository) extends AbstractController(cc) {

  def auth() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.auth())
  }

  def postAuth() = Action { implicit request: Request[AnyContent] =>
    var authRedirect = Redirect(routes.AuthController.auth())

    val form = authForm.bindFromRequest

    if (form.errors.nonEmpty) {
      authRedirect.flashing("error" -> "Login and password are required fields")
    } else {
      val success = usersRepository.checkCredentials(form.get.login, Auth.hashPassword(form.get.password))
      if (success) Redirect(routes.HomeController.index()).withSession("login" -> form.get.login) else authRedirect.flashing("error" -> "Wrong credentials")
    }
  }



  def logout() = Action { implicit request =>
    Redirect(routes.AuthController.auth()).removingFromSession("login")
  }
}
