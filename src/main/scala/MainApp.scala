package zio.crawler
import health.HealthRoute

import zio.crawler.scrape.ScrapeRoute
import zio.crawler.users.InMemoryUsersRepo
import zio.http.Server
import zio.{Scope, ZIO, ZIOAppArgs, ZIOAppDefault}
object MainApp extends ZIOAppDefault {
  override def run: ZIO[Environment with ZIOAppArgs with Scope, Any, Any] = {
    val httpApp = HealthRoute() ++ ScrapeRoute()
    Server
      .serve(
        httpApp.withDefaultErrorResponse
      )
      .provide(
        InMemoryUsersRepo.layer,
        Server.defaultWithPort(6666)
      )
  }
}
