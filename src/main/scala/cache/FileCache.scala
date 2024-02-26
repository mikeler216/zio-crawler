package zio.crawler
package cache

import sttp.model.Uri
import zio.ZIO.sleep
import zio.{durationInt, Task, ZIO}

import java.io.{FileNotFoundException, PrintWriter}
import java.security.MessageDigest

object FileCache extends Cache {
  private val cacheDir = "/tmp"

  val layer = zio.ZLayer.succeed(FileCache)

  override def getUrlsFromCache(url: Uri): ZIO[Any, String, Set[String]] = {
    readFromFile(url).orElseFail("cache file not found")
  }

  override def putUrlsInCache(url: Uri, links: Set[String]): Task[Unit] = {
    writeToFile(url, links) *> ZIO.debug(s"writing to file ${urlFilePath(url)} finished")
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
    ZIO.acquireReleaseWith(ZIO.attempt(new PrintWriter(filePath)))(file => ZIO.attempt(file.write(links.mkString("\n"))).orElseSucceed())(
      file => ZIO.attempt(file.close()) *> cleanUpCacheFile(url).forkDaemon *> ZIO.unit
    )
  }

  private def cleanUpCacheFile(url: Uri): ZIO[Any, Nothing, Unit] = {
    for {
      _ <- sleep(30.second).debug("sleeping for 30 second")
      _ <- deleteFile(url).orElseSucceed("failed to delete file")
      _ <- ZIO.debug(s"file deleted ${urlFilePath(url)}")
    } yield ()
  }

  def deleteFile(url: Uri): Task[Unit] = {
    ZIO.attempt(new java.io.File(urlFilePath(url)).delete()).debug("deleting file") *> ZIO.unit
  }

  def readFromFile(url: Uri): Task[Set[String]] = {
    val filePath = urlFilePath(url)
    for {
      fileExist <- fileExist(url)
      _         <- if (!fileExist) ZIO.fail(new FileNotFoundException("File does not exist")) else ZIO.unit
      file      <- ZIO.attempt(scala.io.Source.fromFile(filePath))
      links     <- ZIO.attempt(file.getLines().toSet)
      _         <- ZIO.attempt()

    } yield links
  }
  def fileExist(url: Uri): Task[Boolean] = {
    val filePath = urlFilePath(url)
    ZIO.attempt {
      new java.io.File(filePath).exists()
    }
  }

}
