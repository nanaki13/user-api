package security

import com.mohiva.play.silhouette.api.{Authenticator, Authorization}
import com.mohiva.play.silhouette.impl.User
import play.api.mvc.Request

import scala.concurrent.Future

case class WithProvider[A <: Authenticator](provider: String) extends Authorization[User, A] {

  def isAuthorized[B](user: User, authenticator: A)(
    implicit request: Request[B]) = {

    Future.successful(user.loginInfo.providerID == provider)
  }
}
