package zio.crawler
package cache

import sttp.model.Uri
import zio.crawler.cache.UrlCache.fileExist
import zio.{RIO, Task, ZIO}

import java.io.{FileNotFoundException, PrintWriter}
import java.security.MessageDigest

object UrlCache {
  private val cacheDir = "/tmp"

  def getUrlsFromCache(url: Uri): ZIO[Any, String, Set[String]] = {
    readFromFile(url).orElseFail("cache file not found")
  }

  def putUrlsInCache(url: Uri, links: Set[String]): RIO[Any, Unit] = {
    writeToFile(url, links)
  }

  private def fileName(url: Uri): String = {
    val hash: String = MessageDigest.getInstance("MD5").digest(url.toString().getBytes).map("%02x".format(_)).mkString
    s"$hash.txt"
  }

  private def urlFilePath(url: Uri): String = {
    s"$cacheDir/${fileName(url)}"
  }

  def writeToFile(url: Uri, links: Set[String]): Task[Unit] = {
    val filePath = urlFilePath(url)
    ZIO.attempt {
      val file = new PrintWriter(filePath)
      file.write(links.mkString("\n"))
      file.close()
    }
  }

  def deleteFile(url: Uri): Task[Unit] = {
    val filePath = urlFilePath(url)
    ZIO.attempt {
      for {
        fileExist <- fileExist(url)
        _         <- if (fileExist) ZIO.attempt(new java.io.File(filePath).delete())
                     else ZIO.unit
      } yield ()
    }
  }

  def readFromFile(url: Uri): Task[Set[String]] = {
    val filePath = urlFilePath(url)
    for {
      fileExist <- fileExist(url)
      _         <- if (!fileExist) ZIO.fail(new FileNotFoundException("File does not exist")) else ZIO.unit
      file      <- ZIO.attempt(scala.io.Source.fromFile(filePath))
      links     <- ZIO.attempt(file.getLines().toSet)
      _         <- ZIO.attempt(file.close())
    } yield links

  }
  def fileExist(url: Uri): Task[Boolean] = {
    val filePath = urlFilePath(url)
    ZIO.attempt {
      new java.io.File(filePath).exists()
    }

  }

}
