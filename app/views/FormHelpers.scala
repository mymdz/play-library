package views

object FormHelpers {
  import views.html.helper.FieldConstructor
  val fields = FieldConstructor(f => html.fieldTemplate(f))
}
