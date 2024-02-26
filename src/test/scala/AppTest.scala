import sttp.client3.UriContext
import zio.crawler.cache.{Cache, FileCache}
import zio.{Scope, Semaphore, UIO, ULayer, ZIO, ZLayer, durationInt}
import zio.crawler.health.HealthRoute
import zio.crawler.scrape.ScrapeRoute
import zio.crawler.scraper.{OnlineScraper, Scraper}
import zio.crawler.users.{InMemoryUsersRepo, User, UsersRepo}
import zio.test._
import zio.test.Assertion.{anything, containsString, equalTo, isSubtype}
import zio.http._

import java.io.PrintWriter

object AppTest extends ZIOSpecDefault {

  object TestScraper extends Scraper {
    val layer: ULayer[TestScraper.type]                           = ZLayer.succeed(this)
    override def scrapeUrls(url: String, depth: Int, semaphore: UIO[Semaphore]): ZIO[Cache, Throwable, Set[String]] = ZIO.succeed(Set.empty)
  }
  def spec: Spec[TestEnvironment with Scope, Any] = suite("http")(
    test("should be ok") {
      val req = Request.get(URL(Root / "health"))
      assertZIO(HealthRoute().runZIO(req))(equalTo(Response.ok))
    },
    test("bad query params for scraper") {
      val req = Request.get(URL(Root / "scrape", queryParams = QueryParams("url" -> "http://www.google.com")))
      assertZIO(ScrapeRoute().runZIO(req))(equalTo(Response.fromHttpError(HttpError.BadRequest())))
    },
    test("run scraper") {
      val body = OnlineScraper.getUrlHTML(uri"http://example.org").flatMap(html => ZIO.succeed(html))
      assertZIO(body)(containsString("html"))
    },
    test("parse links from html") {
      val links = OnlineScraper.parseNewLinks("<a href='http://www.google.com'>Google</a>")
      assertZIO(links)(equalTo(Set("http://www.google.com")))
    },
    test("get HTML and parse Links") {
      val links = OnlineScraper.getHTMLAndParseLinks(uri"http://example.org")
      assertZIO(links)(equalTo(Set("https://www.iana.org/domains/example")))
    },
    test("scrape") {
      assertZIO {
        for {
          links <- OnlineScraper.scrapeUrls("http://example.org", 3, Semaphore.make(1))

        } yield links
      }(equalTo(Set.empty[String]))
    }
  ).provideLayer(InMemoryUsersRepo.layer ++ TestScraper.layer ++ FileCache.layer)
}

object TestCache extends ZIOSpecDefault {
  def spec: Spec[TestEnvironment with Scope, Any] = suite("cache")(
    // clean up the file after the test
    test("should write file and exist") {
      val testUri = uri"http://google.org"
      assertZIO {
        for {
          _      <- FileCache.writeToFile(testUri, Set("https://www.iana.org/domains/example"))
          exists <- FileCache.fileExist(testUri)
          _      <- FileCache.deleteFile(testUri)
        } yield exists
      }(equalTo(true))
    },
    test("should clean up file after test") {
      val testUri = uri"http://example.org"
      assertZIO {
        for {
          _      <- FileCache.writeToFile(testUri, Set("https://www.iana.org/domains/example"))
          _      <- TestClock.adjust(1.minute)
          exists <- FileCache.fileExist(testUri)
        } yield exists
      }(equalTo(false))
    },
    test("should read file") {
      val testUri = uri"http://example.org"
      assertZIO {
        for {
          _     <- FileCache.writeToFile(testUri, Set("https://www.iana.org/domains/example"))
          links <- FileCache.readFromFile(testUri)
          _     <- FileCache.deleteFile(testUri)
        } yield links
      }(equalTo(Set("https://www.iana.org/domains/example")))
    }
  )
}

object TestUser extends ZIOSpecDefault {

  def spec: Spec[TestEnvironment with Scope, Any] = suite("user")(
    test("register user test") {
      assertZIO {
        for {
          zSemaphore <- UsersRepo.getOrSetUserSemaphore("test")
          semaphore  <- zSemaphore
        } yield semaphore
      }(isSubtype[Semaphore](anything))
    },
    test("user cleanup") {
      assertZIO {
        for {
          _          <- UsersRepo.getOrSetUserSemaphore("test")
          zSemaphore <- UsersRepo.getOrSetUserSemaphore("test")
          semaphore  <- zSemaphore
        } yield semaphore
      }(isSubtype[Semaphore](anything))
    }
  ).provideLayer(InMemoryUsersRepo.layer)
}
