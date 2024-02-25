package zio.crawler
package health

import zio.http._

object HealthRoute {
  def apply(): Http[Any, Nothing, Request, Response] = {
    Http.collect[Request] {
      case Method.GET -> Root / "health" => Response.ok
    }
  }

}
