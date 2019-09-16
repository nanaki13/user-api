package repository

import akka.actor.ActorSystem
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import javax.inject.Inject
import model.{Role, User}
import play.api.libs.concurrent.CustomExecutionContext

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import security.Provider

import scala.reflect.ClassTag

class UserRepositoryImpl @Inject()(implicit val userExecutionContext: UserExecutionContext)  extends  DelegableAuthInfoDAO[PasswordInfo] with UserRepository {
  def findByLoginInfo(loginInfo: LoginInfo): Future[Option[User]] = Future {
    println(users)
    println(loginInfo)
    val f = users.find(_.email == loginInfo.providerKey)
    println(f)
    f
  }

  def create(user: User): Future[User] = Future {
    val userNEw = new User(users.map(_.id).max + 1,user.login,user.email ,user.loginInfo,Role.USER)
    users += userNEw
    println(users)

    userNEw
  }

  def update(user: User): Future[Boolean] = Future {
    users.zipWithIndex.find(_._1.email == user.loginInfo.providerKey) match {
      case Some(index) => users(index._2) =  user ; true
      case None => false
    }
  }

  val users = ListBuffer[User]()
  def find( id : Int) : Future[Option[User]] = Future {
    users.find(_.id == id)
  }
  def all( ) : Future[Iterable[User]] = Future {
    users
  }

  /**
    * Saves the password info.
    *
    * @param loginInfo The login info for which the auth info should be saved.
    * @param authInfo The password info to save.
    * @return The saved password info or None if the password info couldn't be saved.
    */
  def save(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = {
    val  ret = findByLoginInfo(loginInfo) flatMap  {
      case Some(user) =>
        val updated = user.copy(
        id = user.id,
        email = user.email,
        login = user.login,
        loginInfo=user.loginInfo,
        passwordInfo = Some(authInfo)
         )
        update(updated).map(_ => updated.passwordInfo.get)
      case None => Future.failed(new IdentityNotFoundException("cant find user"))
    }
    ret
  }

  /**
    * Finds the password info which is linked with the specified login info.
    *
    * @param loginInfo The linked login info.
    * @return The retrieved password info or None if no password info could be retrieved for the given login info.
    */
  def find(loginInfo: LoginInfo): Future[Option[PasswordInfo]] = {
    findByLoginInfo(loginInfo).map{
      case Some(user) => user.passwordInfo
      case None => None
    }
  }



  override def add(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = save(loginInfo, authInfo)

  override def update(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = save(loginInfo, authInfo)

  override def remove(loginInfo: LoginInfo): Future[Unit] = Future.successful(Unit)
  val classTag: ClassTag[PasswordInfo] = ClassTag(classOf[PasswordInfo])


}
trait UserRepository {
  def findByLoginInfo(loginInfo: LoginInfo): Future[Option[User]]

  def create(user: User): Future[User]

  def update(user: User): Future[Boolean]


  def find(id: Int): Future[Option[User]]

  def all(): Future[Iterable[User]]

  /**
    * Saves the password info.
    *
    * @param loginInfo The login info for which the auth info should be saved.
    * @param authInfo  The password info to save.
    * @return The saved password info or None if the password info couldn't be saved.
    */
  def save(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo]

  /**
    * Finds the password info which is linked with the specified login info.
    *
    * @param loginInfo The linked login info.
    * @return The retrieved password info or None if no password info could be retrieved for the given login info.
    */
  def find(loginInfo: LoginInfo): Future[Option[PasswordInfo]]
}
class UserExecutionContext @Inject()(actorSystem: ActorSystem)
  extends CustomExecutionContext(actorSystem, "repository.user.dispatcher")
