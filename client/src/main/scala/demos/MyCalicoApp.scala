package demos

import calico.html.io.{*, given}
import cats.effect.*
import fs2.*
import fs2.dom.*

/**
 * Demo of a simple web app using Calico.
 * Uses the IOWebApp trait to provide a render method.
 * Defaults to using the IO monad, but can be overridden.
 * Default rootElementId is also provided as a "app", but can be overridden.
 * */
object MyCalicoApp extends IOWebApp:
  def render: Resource[IO, HtmlElement[IO]] =
    div("Toto, I've a feeling we're not in Kansas anymore.")