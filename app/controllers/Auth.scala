package controllers

import java.security.MessageDigest

import play.api.Play
import play.api.data.Form
import play.api.data.Forms._
import play.mvc.Http.Request

object Auth {


  case class LoginFormData(login: String, password: String)

  val authForm = Form(
    mapping(
      "login" -> nonEmptyText,
      "password" -> nonEmptyText
    )(LoginFormData.apply)(LoginFormData.unapply)
  )

  def hashPassword(password: String) = md5(password)

  def md5(str: String) = {
    val md5 = MessageDigest.getInstance("MD5").digest(str.getBytes)
    var md5Str = ""

    for (md5_byte <- md5) {
      md5Str = md5Str + String.format("%02x", Byte.box(md5_byte))
    }

    md5Str
  }

}
