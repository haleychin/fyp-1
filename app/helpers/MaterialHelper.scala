package helpers

object MaterialHelper {
  import views.html.helper.FieldConstructor
  implicit val myFields = FieldConstructor(views.html.component.input.f)
}
