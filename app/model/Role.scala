package model

import play.api.libs.json.{Format, Json}
import Role._

trait WithRole[RoleId] {
  def role:Role
  def id : RoleId
  def canRead(role : Role): Boolean ={
    this.role.canRead(role)
  }
  def canUpdate(role : Role): Boolean ={
    this.role.canUpdate(role)
  }
  def canDelete(role : Role): Boolean ={
    this.role.canDelete(role)
  }
  def canCreate(role : Role): Boolean ={
    this.role.canCreate(role)
  }

  def canRead(other : WithRole[RoleId]): Boolean ={
    this.role.canRead(other.role) || id == other.id
  }
  def canUpdate(other :  WithRole[RoleId]): Boolean ={
    this.role.canUpdate(other.role) || id == other.id
  }
  def canDelete(other :  WithRole[RoleId]): Boolean ={
    this.role.canDelete(other.role) || id == other.id
  }

}
case class Role(role : String){

  def canRead(role : Role): Boolean ={
    this > role
  }
  def canUpdate(role : Role): Boolean ={
    this > role
  }
  def canCreate(role : Role): Boolean ={
    this > role
  }
  def canDelete(role : Role): Boolean ={
    this > role
  }

  def canRead(): Boolean ={
    true
  }
  def canUpdate(): Boolean ={
    true
  }
  def canCreate(): Boolean ={
    true
  }
  def canDelete(): Boolean ={
    this > USER
  }

  def > (role: Role): Boolean = {
    this == ADMIN && role != ADMIN
  }
}
object Role{

  implicit def ordering  : Ordering[Role] =  (x: Role, y: Role) => if (x > y ) 1 else if( x == y) 0 else -1
  val ADMIN = Role("admin")
  val USER = Role("user")

  implicit val format :Format[Role]= Json.format
}
