package repository

import akka.actor.ActorSystem
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.PasswordInfo
import javax.inject.Inject
import model.User
import play.api.libs.concurrent.CustomExecutionContext

import scala.concurrent.Future
import scala.reflect.ClassTag
trait UserRepository {
  val classTag: ClassTag[PasswordInfo] = ClassTag(classOf[PasswordInfo])
  def delete(idInt: Int): Future[Boolean]


  def removeUser(loginInfo: LoginInfo): Future[Boolean]
  def findByLoginInfo(loginInfo: LoginInfo): Future[Option[User]]

  def create(user: User): Future[User]

  def update(user: User): Future[Option[User]]


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




