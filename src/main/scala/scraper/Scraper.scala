package zio.crawler
package scraper

import zio.ZIO.serviceWithZIO
import zio.{Semaphore, UIO, ZIO}

trait Scraper {
  def scrapeUrls(url: String, depth: Int, semaphore: UIO[Semaphore]): ZIO[Any, Throwable, Set[String]] = ???

}

object Scraper {
  def scrapeUrls(url: String, depth: Int, semaphore: UIO[Semaphore]): ZIO[Scraper, Throwable, Set[String]] = {
    serviceWithZIO[Scraper](_.scrapeUrls(url, depth, semaphore))
  }
}
