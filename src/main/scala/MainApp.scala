package zio.crawler
import health.HealthRoute

import cache.FileCache
import scrape.ScrapeRoute
import scraper.OnlineScraper
import users.InMemoryUsersRepo
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
        InMemoryUsersRepo.layer ++
          OnlineScraper.layer ++
          FileCache.layer ++
        Server.defaultWithPort(6666)
      )
  }
}
