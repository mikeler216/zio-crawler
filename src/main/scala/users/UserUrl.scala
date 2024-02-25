package zio.crawler
package users

import zio.json.{DeriveJsonEncoder, JsonEncoder}

case class UserUrl(url: String, userID: String)

object UserUrl {
  implicit val encoder: JsonEncoder[UserUrl] = DeriveJsonEncoder.gen[UserUrl]
}
