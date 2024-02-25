package zio.crawler
package scraper

import sttp.client3.httpclient.zio.HttpClientZioBackend
import sttp.client3._
import sttp.model.Uri
import cache.UrlCache.{getUrlsFromCache, putUrlsInCache}

import zio.{Semaphore, UIO, ZIO}

import scala.util.Try

object Scraper {

  def getUrlHTML(url: Uri): ZIO[Any, Throwable, String]                = {
    HttpClientZioBackend().flatMap(backend => {
      val request = basicRequest.get(url).response(asStringAlways)
      val body    = backend.send(request).map(_.body)
      body
    })
  }
  def parseNewLinks(html: String): UIO[Set[String]]                    = {
    ZIO.succeed {
      val pattern = """(?i)<a[^>]+?href\s*=\s*['"](https?://[^'"]+)['"][^>]*>""".r
      Try {
        pattern.findAllMatchIn(html).map(_.group(1)).toSet
      }.getOrElse(Set.empty)
    }
  }
  def getHTMLAndParseLinks(url: Uri): ZIO[Any, Throwable, Set[String]] = {
    getUrlHTML(url).flatMap(parseNewLinks)
  }

  def strToUri(str: String): ZIO[Any, Exception, Uri] = Uri.parse(str) match {
    case Right(uri) => ZIO.succeed(uri)
    case Left(_)    => ZIO.fail(new Exception(s"Could not parse $str to Uri"))
  }


  def scrapeUrls(url: String, depth: Int, semaphore: UIO[Semaphore]): ZIO[Any, Throwable, Set[String]] = {


    def loop(url: Uri, depth: Int, visited: Set[Uri]): ZIO[Any, Throwable, Set[String]] = {
      if (depth <= 0 || visited.contains(url)) ZIO.succeed(Set.empty)
      else
        for {
          links         <- getUrlsFromCache(url).orElse(getHTMLAndParseLinks(url).orElse(ZIO.succeed(Set.empty))).debug("links")
          newLinks       = links.filter(!_.contains(visited)).map { uri => Uri.parse(uri).getOrElse(uri"") }
          _             <- putUrlsInCache(url, newLinks.map(_.toString))
          nvisited       = visited + url
          userSemaphore <- semaphore
          _             <- userSemaphore.withPermit {
                             ZIO.foreachPar(newLinks)(uri => loop(uri, depth - 1, nvisited))
                           }
        } yield Set.empty
    }
    val uri = Uri.parse(url).getOrElse(throw new Exception("Could not parse url"))
    loop(uri, depth, Set.empty)
  }

}
