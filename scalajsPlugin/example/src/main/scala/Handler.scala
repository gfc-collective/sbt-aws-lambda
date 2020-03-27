package example

import scala.scalajs.js.annotation.{ JSExport, JSExportTopLevel }

@JSExportTopLevel("Handler")
class Handler {

  @JSExport
  def foo(): Unit =
    println("Foo!")

  @JSExport
  def bar(): Unit =
    println("Bar!")

}