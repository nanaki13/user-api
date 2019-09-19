package model

import play.api.libs.json.{Format, Json}

case class Role(role : String)
object Role{
  val ADMIN = Role("admin")
  val USER = Role("user")
  // val ADMIN = Role("admin")
  implicit val format :Format[Role]= Json.format
}
