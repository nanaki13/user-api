package repository

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.{PasswordHasherRegistry, PasswordInfo}
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import javax.inject.Inject
import model.User
import play.api.Configuration

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.reflect.ClassTag

case class InMemoryUserRepositoryImpl @Inject()( init: InitRepoBase)(implicit val userExecutionContext: UserExecutionContext)  extends  DelegableAuthInfoDAO[PasswordInfo] with UserRepository {
  val noneId = -1
  def findByLoginInfo(loginInfo: LoginInfo): Future[Option[User]] = Future {
    users.find(_.email == loginInfo.providerKey)

  }

  def create(user: User): Future[User] = Future {

    val id = if (users.isEmpty && (user.id == noneId)) 1 else {
      if(user.id == noneId){
        users.map(_.id).max + 1
      }else{
        noneId
      }
    }
    val userNEw = new User(id,user.login,user.email ,user.loginInfo,user.role,user.passwordInfo)
    users += userNEw
    userNEw
  }

  def update(user: User): Future[Option[User]] = Future {
    users.zipWithIndex.find(_._1.id == user.id) match {
      case Some(index) => users(index._2) =  user ; Some(user.copy())
      case _ => None
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
    * As this is also UserRepository, we already create the user before and we can find it by loginInfo
    * and update
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
      case Some(user) => {user.passwordInfo}
      case None => None
    }
  }


  /**
    * Map to save
    * @param loginInfo
    * @param authInfo
    * @return
    */
  override def add(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = save(loginInfo, authInfo)

  /**
    * Map to save
    * @param loginInfo
    * @param authInfo
    * @return
    */
  override def update(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = save(loginInfo, authInfo)




  override def removeUser(loginInfo: LoginInfo): Future[Boolean] =  {
    val fin = users.zipWithIndex.find(_._1.loginInfo == loginInfo)
    fin match {
      case Some(u) => users.remove( u._2);Future.successful(true)
      case _ => ;Future.successful(false)
    }

  }

  /**
    * As we are the user repo, we update to None
    * @param loginInfo
    * @return
    */
  override def remove(loginInfo: LoginInfo): Future[Unit] = {
    findByLoginInfo(loginInfo).map{
      case Some(user) => update(user.copy(passwordInfo = None))
      case None => Future.failed(new IdentityNotFoundException( s"can't find $loginInfo"))
    }

  }



  override def delete(idInt: Int): Future[Boolean] = {
    val fin = users.zipWithIndex.find(_._1.id == idInt)
    fin match {
      case Some(u) => users.remove( u._2);Future.successful(true)
      case _ => ;Future.successful(false)
    }
  }

  val res = Await.result(init.init(this),Duration.Inf)
}

case class InitImemory @Inject()( hasher: PasswordHasherRegistry,
 executionContext: UserExecutionContext,
 configuration: Configuration) extends InitRepoBase {
  def init(implicit userRepository: UserRepository): Future[Boolean] = createAdmin
}
