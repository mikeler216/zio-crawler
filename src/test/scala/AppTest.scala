import sttp.client3.UriContext
import zio.crawler.cache.UrlCache
import zio.{Scope, Semaphore, ZIO}
import zio.crawler.health.HealthRoute
import zio.crawler.scrape.ScrapeRoute
import zio.crawler.scraper.Scraper
import zio.crawler.users.{InMemoryUsersRepo, UsersRepo}
import zio.test._
import zio.test.Assertion.{anything, containsString, equalTo, isSubtype}
import zio.http._

object AppTest extends ZIOSpecDefault {

  def spec: Spec[TestEnvironment with Scope, Any] = suite("http")(
    test("should be ok") {
      val req = Request.get(URL(Root / "health"))
      assertZIO(HealthRoute().runZIO(req))(equalTo(Response.ok))
    },
    test("should handle another case") {
      val req = Request.get(URL(Root / "scrape", queryParams = QueryParams("url" -> "http://www.google.com", "userId" -> "123")))
//      assertZIO(ScrapeRoute().runZIO(req))(equalTo(Response.text(s"Scraped http://www.google.com with userID 123")))
      assertZIO(ScrapeRoute().runZIO(req))(equalTo(Response.json("""{"url":"http://www.google.com","userID":"123"}""")))
    },
    test("bad query params for scraper") {
      val req = Request.get(URL(Root / "scrape", queryParams = QueryParams("url" -> "http://www.google.com")))
      assertZIO(ScrapeRoute().runZIO(req))(equalTo(Response.fromHttpError(HttpError.BadRequest())))
    },
    test("run scraper") {
      val body = Scraper.getUrlHTML(uri"http://example.org").flatMap(html => ZIO.succeed(html))
      assertZIO(body)(containsString("html"))
    },
    test("parse links from html") {
      val links = Scraper.parseNewLinks("<a href='http://www.google.com'>Google</a>")
      assertZIO(links)(equalTo(Set("http://www.google.com")))
    },
    test("get HTML and parse Links") {
      val links = Scraper.getHTMLAndParseLinks(uri"http://example.org")
      assertZIO(links)(equalTo(Set("https://www.iana.org/domains/example")))
    },
    test("scrape") {
      assertZIO {
        for {
          links <- Scraper.scrapeUrls(uri"http://example.org", 3, Semaphore.make(1))

        } yield links
      }(equalTo(Set.empty[String]))
    }
  ).provideLayer(InMemoryUsersRepo.layer)
}

object TestCache extends ZIOSpecDefault {
  def spec: Spec[TestEnvironment with Scope, Any] = suite("cache")(
    // clean up the file after the test
    test("should write file and exist") {
      val testUri = uri"http://google.org"
      assertZIO {
        for {
          _      <- UrlCache.writeToFile(testUri, Set("https://www.iana.org/domains/example"))
          exists <- UrlCache.fileExist(testUri)
          _      <- UrlCache.deleteFile(testUri)
        } yield exists
      }(equalTo(true))
    },
    test("should read file") {
      val testUri = uri"http://example.org"
      assertZIO {
        for {
          _     <- UrlCache.writeToFile(testUri, Set("https://www.iana.org/domains/example"))
          links <- UrlCache.readFromFile(testUri)
          _     <- UrlCache.deleteFile(testUri)
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
          semaphore <- zSemaphore
        } yield semaphore
      }(isSubtype[Semaphore](anything))
    }
  ).provideLayer(InMemoryUsersRepo.layer)
}
