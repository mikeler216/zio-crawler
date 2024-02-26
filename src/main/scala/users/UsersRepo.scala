package zio.crawler
package users

import zio.{Semaphore, UIO, ZIO}

trait UsersRepo {
  def getOrSetUserSemaphore(userId: String): ZIO[Any, Throwable, UIO[Semaphore]]
  def getUser(userId: String): ZIO[Any, Any, Option[User]]
}

object UsersRepo {
  def getOrSetUserSemaphore(userId: String): ZIO[UsersRepo, Throwable, UIO[Semaphore]] = {
    ZIO.serviceWithZIO[UsersRepo](_.getOrSetUserSemaphore(userId))
  }
  def getUser(userId: String): ZIO[UsersRepo, Nothing, ZIO[Any, Any, Option[User]]] = {
    ZIO.serviceWith[UsersRepo](_.getUser(userId))
  }
}
