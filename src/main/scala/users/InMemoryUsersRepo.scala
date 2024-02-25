package zio.crawler
package users

import zio.ZIO.sleep
import zio._

import scala.collection.mutable

case class InMemoryUsersRepo(usersMap: Ref[mutable.Map[String, UIO[Semaphore]]]) extends UsersRepo {
  private val numOfThreads: Int                                             = 5
  private def register(userId: String): ZIO[Any, Throwable, UIO[Semaphore]] = {
    for {
      map      <- usersMap.get
      exists   <- userExists(userId)
      _         = if (exists) ZIO.fail(throw new Exception("User already exists"))
      semaphore = Semaphore.make(numOfThreads)
      _        <- ZIO.succeed(map.addOne(userId, semaphore)).debug("user added")
      _        <- usersMap.set(map)
    } yield semaphore
  }

  private def deleteUser(userId: String): ZIO[Any, Throwable, Unit] = {
    for {
      map <- usersMap.get
      _   <- assertUserExists(userId)
      _   <- ZIO.succeed(map.remove(userId))
      _   <- usersMap.set(map)
    } yield ()
  }

  private def assertUserExists(userId: String): Task[Unit] = {
    for {
      map <- usersMap.get
      _    = if (!map.contains(userId)) ZIO.fail(new Exception("User does not exist"))
    } yield ()
  }

  private def userExists(userId: String): UIO[Boolean] = {
    for {
      map <- usersMap.get
    } yield map.contains(userId)
  }

  private def getUserSemaphore(userId: String): ZIO[Any, Throwable, UIO[Semaphore]] = {
    for {
      map <- usersMap.get
      _   <- assertUserExists(userId)
    } yield map(userId)
  }

  def getOrSetUserSemaphore(userId: String): ZIO[Any, Throwable, UIO[Semaphore]] = {
    for {
      exists <- userExists(userId)
      sem    <- if (!exists) register(userId) else getUserSemaphore(userId).debug("user exists")
//      _ = ZIO.acquireRelease(sem)(_ => ZIO.succeed{
//        sleep(5.seconds).debug(s"releasing user $userId")
//        deleteUser(userId)
//      }
//      )
    } yield sem
  }
}

object InMemoryUsersRepo {
  def layer: ZLayer[Any, Nothing, InMemoryUsersRepo] =
    ZLayer.fromZIO(
      Ref.make(mutable.Map.empty[String, UIO[Semaphore]]).map(new InMemoryUsersRepo(_))
    )
}
