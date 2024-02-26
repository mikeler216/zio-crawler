package zio.crawler
package users

import zio._

import java.time.Instant
import scala.collection.mutable

case class User(ttl: Instant, semaphore: UIO[Semaphore])

case class InMemoryUsersRepo(usersMap: Ref[mutable.Map[String, User]]) extends UsersRepo {
  private val numOfThreads: Int                                             = 5
  private val userTTL: Duration                                             = 30.seconds
  private def register(userId: String): ZIO[Any, Throwable, UIO[Semaphore]] = {
    for {
      map      <- usersMap.get
      exists   <- userExists(userId)
      _         = if (exists) ZIO.fail(throw new Exception("User already exists"))
      semaphore = Semaphore.make(numOfThreads)
      user      = User(ttl = Instant.now() plus userTTL, semaphore = semaphore)
      _        <- ZIO.succeed(map.addOne((userId, user))).debug("user added")
      _        <- usersMap.set(map)
    } yield semaphore
  }

  private def deleteUser(userId: String): ZIO[Any, Throwable, Unit] = {
    for {
      _   <- ZIO.debug(s"deleting user $userId")
      map <- usersMap.get
      _   <- assertUserExists(userId)
      _   <- ZIO.succeed(map.remove(userId))
      _   <- usersMap.set(map)
    } yield ()
  }

  private def deleteUserAndWait(userId: String): Task[Unit] = {
    for {
      _        <- ZIO.sleep(userTTL).debug("waiting for user to expire")
      _        <- ZIO.debug(s"starting deleting user $userId")
      _        <- assertUserExists(userId)
      map      <- usersMap.get
      ttl       = map(userId).ttl
      isExpired = Instant.now().isAfter(ttl)
      _        <- if (isExpired) deleteUser(userId) else deleteUserAndWait(userId).forkDaemon.debug("user not expired")
    } yield ()
  }

  private def registerAndCleanup(userId: String): ZIO[Any, Throwable, UIO[Semaphore]] = {
    ZIO.acquireReleaseWith(register(userId))(_ => deleteUserAndWait(userId).forkDaemon *> ZIO.unit) { sem => ZIO.succeed(sem) }
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
      user = map(userId)
      _    = map.addOne(userId, User(Instant.now().plus(userTTL), user.semaphore))
    } yield user.semaphore
  }

  def getOrSetUserSemaphore(userId: String): ZIO[Any, Throwable, UIO[Semaphore]] = {
    for {
      exists <- userExists(userId)
      sem    <- if (!exists) registerAndCleanup(userId) else getUserSemaphore(userId).debug("user exists")
    } yield sem
  }

  override def getUser(userId: String): ZIO[Any, Any, Option[User]] = {
    for {
      map    <- usersMap.get
      user = map.get(userId)
    }  yield user

  }

}

object InMemoryUsersRepo {
  def layer: ZLayer[Any, Nothing, InMemoryUsersRepo] =
    ZLayer.fromZIO(
      Ref.make(mutable.Map.empty[String, User]).map(new InMemoryUsersRepo(_))
    )
}
