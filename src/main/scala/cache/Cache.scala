package zio.crawler
package cache

import sttp.model.Uri
import zio.ZIO.serviceWithZIO
import zio.{Scope, Task, ZIO}

trait Cache {

  def getUrlsFromCache(url: Uri): ZIO[Any, String, Set[String]] = ???
  def putUrlsInCache(url: Uri, links: Set[String])   : Task[Unit]           = ???
}

object Cache {
  def getUrlsFromCache(url: Uri): ZIO[Cache, String, Set[String]] = serviceWithZIO[Cache](_.getUrlsFromCache(url))
  def putUrlsInCache(url: Uri, links: Set[String]): ZIO[Cache, Throwable, Unit] = serviceWithZIO[Cache](_.putUrlsInCache(url, links))
}
