package model

import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.api.{Identity, LoginInfo}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import security.Provider

final case class User(id: Int, pseudonym : String ,email: String , loginInfo: LoginInfo,role: Role,passwordInfo: Option[PasswordInfo] = None) extends Identity with WithRole[Int]
object User  {
//  implicit val noFormat: Format[Option[_]] =

  def apply(pseudonym : String ,email: String ) : User = {
    User(-1,pseudonym,email, LoginInfo(Provider.name,email),Role.USER)
  }

  def apply(pseudonym : String ,email: String, loginInfo: LoginInfo ) : User = {
    User(-1,pseudonym,email,loginInfo,Role.USER)
  }

  def apply(id: Int,pseudonym : String ,email: String ) : User = {
    User(-1,pseudonym,email, LoginInfo(Provider.name,email),Role.USER)
  }
  implicit val customReads: Reads[User] = (
    (JsPath \ "pseudonym").read[String] and
      (JsPath \ "email").read[String]
    )((pseudonym, email) => User(pseudonym,email))

  implicit val customWrites: Writes[User] = (
    (JsPath \ "pseudonym").write[String] and
      (JsPath \ "email").write[String]
    )((user) => (user.pseudonym,user.email))

  implicit val formatPass :Format[PasswordInfo]= Json.format[PasswordInfo]
  implicit val format :Format[User]= Json.format[User]

}
