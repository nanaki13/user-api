package model

case class Role(role : String) {

}

object Role{
  val ADMIN = Role("admin")
  val USER = Role("user")
  // val ADMIN = Role("admin")
}
