package model

import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.api.{Identity, LoginInfo}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import security.Provider

final case class User(id: Int, login : String ,email: String , loginInfo: LoginInfo,role: Role,passwordInfo: Option[PasswordInfo] = None) extends Identity

object User{
//  implicit val noFormat: Format[Option[_]] =
//  implicit val format :Format[User]= Json.format
  def apply(login : String ,email: String ) : User = {
    User(-1,login,email, LoginInfo(Provider.name,email),Role.USER)
  }

  def apply(login : String ,email: String, loginInfo: LoginInfo ) : User = {
    User(-1,login,email,loginInfo,Role.USER)
  }

  def apply(id: Int,login : String ,email: String ) : User = {
    User(-1,login,email, LoginInfo(Provider.name,email),Role.USER)
  }
  implicit val customReads: Reads[User] = (
    (JsPath \ "login").read[String] and
      (JsPath \ "email").read[String]
    )((login, email) => User(login,email))

  implicit val customWrites: Writes[User] = (
    (JsPath \ "login").write[String] and
      (JsPath \ "email").write[String]
    )((user) => (user.login,user.email))
}
