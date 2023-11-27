package demos

import calico.syntax.*
import calico.unsafe.given
import cats.effect.IO
import cats.effect.Resource
import fs2.dom.Window
import org.scalajs.dom.HTMLElement

trait IOWebApp:

  def rootElementId: String = "app"

  val window: Window[IO] = Window[IO]

  def render: Resource[IO, fs2.dom.HtmlElement[IO]]

  def main(args: Array[String]): Unit =

    val rootElement = window.document.getElementById(rootElementId)
    val pureRoot = rootElement.flatMap {
      case Some(element) => IO.pure(element)
      case None => IO.raiseError(new NoSuchElementException(s"Element not found check if $rootElementId exists in the html"))
    }

    pureRoot.flatMap(render.renderInto(_).useForever).unsafeRunAndForget()
