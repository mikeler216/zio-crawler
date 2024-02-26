package zio.crawler
package scraper

import zio.ZIO.serviceWithZIO
import cache.Cache
import zio.{Semaphore, UIO, ZIO}

trait Scraper {
  def scrapeUrls(url: String, depth: Int, semaphore: UIO[Semaphore]): ZIO[Cache, Throwable, Set[String]]

}

object Scraper {
  def scrapeUrls(url: String, depth: Int, semaphore: UIO[Semaphore]): ZIO[Scraper with Cache, Throwable, Set[String]] = {
    serviceWithZIO[Scraper](_.scrapeUrls(url, depth, semaphore))
  }
}
