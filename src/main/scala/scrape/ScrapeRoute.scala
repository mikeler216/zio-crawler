package zio.crawler
package scrape

import users.UsersRepo
import scraper.Scraper

import zio.ZIO
import zio.crawler.cache.Cache
import zio.http.Status.BadRequest
import zio.http._

object ScrapeRoute {

  private def parseQueryParams(queryParams: QueryParams): Option[(String, String)] = {
    for {
      url    <- queryParams.get("url").map(_.mkString)
      userId <- queryParams.get("userId").map(_.mkString)
    } yield (url, userId)
  }

  def apply(): Http[UsersRepo with Scraper with Cache, Throwable, Request, Response] = {
    Http.collectZIO[Request] {

      case req @ (Method.GET -> Root / "scrape") =>
        {
          (for {
            _ <- parseQueryParams(req.url.queryParams) match {
                   case Some((url, userId)) =>
                     for {
                       semaphore <- UsersRepo.getOrSetUserSemaphore(userId)
                       _         <- Scraper.scrapeUrls(url, 3, semaphore)
                       _ <- ZIO.debug("request done ")
                     } yield ()
                   case None                => ZIO.fail(HttpError.BadRequest(""))
                 }

          } yield (Response.text("ok"))).catchAll {
            case HttpError.BadRequest("") => ZIO.succeed(Response.fromHttpError(HttpError.BadRequest("")))
            case e                        => ZIO.succeed(Response.fromHttpError(HttpError.InternalServerError(e.getMessage)))
          }
        }

    }
  }

}
