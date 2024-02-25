package zio.crawler
package scrape

import scraper.Scraper.{scrapeUrls}
import users.{UsersRepo}

import zio.{Semaphore, ZIO}
import zio.http._

object ScrapeRoute {

  private def parseQueryParams(queryParams: QueryParams): Option[(String, String)] = {
    for {
      url    <- queryParams.get("url").map(_.mkString)
      userId <- queryParams.get("userId").map(_.mkString)
    } yield (url, userId)
  }

  def apply(): Http[UsersRepo, Throwable, Request, Response] = {
    Http.collect[Request] {
      case req @ (Method.GET -> Root / "scrape") =>
        {
          (for {
            _ <- parseQueryParams(req.url.queryParams) match {
                   case Some((url, userId)) =>
                     for {
                       semaphore <- UsersRepo.getOrSetUserSemaphore(userId)
                       _         <- scrapeUrls(url, 3, semaphore)
                     } yield ()
                   case None                => ZIO.fail(HttpError.BadRequest(""))
                 }

          } yield ()).catchAll {
            case HttpError.BadRequest(_) => ZIO.succeed(Response.fromHttpError(HttpError.BadRequest("Invalid query parameters")))
            case _                       => ZIO.succeed(Response.fromHttpError(HttpError.InternalServerError("Failed to scrape")))
          }
        }
        Response.text("Scraping...")
    }
  }

}
