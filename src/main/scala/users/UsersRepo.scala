package zio.crawler
package users

import zio.{Semaphore, Task, UIO, ZIO}

trait UsersRepo {
  def getOrSetUserSemaphore(userId: String): ZIO[Any, Throwable, UIO[Semaphore]]
}

object UsersRepo {
  def getOrSetUserSemaphore(userId: String): ZIO[UsersRepo, Throwable, UIO[Semaphore]] = {
    ZIO.serviceWithZIO[UsersRepo](_.getOrSetUserSemaphore(userId))
  }
}
