package repository

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordHasherRegistry
import javax.inject.{Inject, Singleton}
import model.{Role, User}
import play.api.Configuration
import security.Provider
import play.api.inject.ApplicationLifecycle
import InitRepo._

import scala.concurrent.Future

/**
  * Init an admin if not
  */
trait InitRepo[-A]{
def init(implicit userRepository: A):Future[Boolean]


}
trait InitRepoBase extends  InitRepo[UserRepository]{
  type  Type = UserRepository
implicit def hasher: PasswordHasherRegistry
implicit def executionContext: UserExecutionContext
implicit def configuration: Configuration

  def createAdmin(implicit userRepository: Type):Future[Boolean] = {
    userRepository.all().map(_.find(_.role == Role.ADMIN)) flatMap  { admins =>
      if (admins.isEmpty) {
        userRepository.create(User(1, adminLogin, adminEmail, LoginInfo(Provider.name, adminEmail), Role.ADMIN, Some(hasher.current.hash(adminPassword)))).map( _ => true)

      }else{
        Future.successful(false)
      }
    }
  }


  def init(implicit userRepository: UserRepository):Future[Boolean]

}

object InitRepo {
  def adminLogin(implicit configration: Configuration) = configration.get[String]("application.admin.login")

  def adminPassword(implicit configration: Configuration) = configration.get[String]("application.admin.password")

  def adminEmail(implicit configration: Configuration) = configration.get[String]("application.admin.email")
}
