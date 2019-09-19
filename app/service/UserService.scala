package service

import com.mohiva.play.silhouette.api.{HandlerResult, LoginInfo}
import com.mohiva.play.silhouette.api.services.IdentityService
import controller.UserControllerComponents
import javax.inject.Inject
import model.{User, UserInputForm}
import repository.UserRepository

import scala.concurrent.Future

class UserService@Inject()(userRepository: UserRepository)extends IdentityService[User] {
  def delete(idInt: Int): Future[Boolean] = {
    userRepository.delete(idInt)
  }

  def find(id: Int) = userRepository.find(id)

  def update(user: User) = userRepository.update(user)


  /**
    * Retrieves a user that matches the specified login info.
    *
    * @param loginInfo The login info to retrieve a user.
    * @return The retrieved user or None if no user could be retrieved for the given login info.
    */
  def retrieve(loginInfo: LoginInfo): Future[Option[User]] = userRepository.findByLoginInfo(loginInfo)

  def create(loginInfo: LoginInfo,input: UserInputForm): Future[User] = {
    userRepository.create(User(input.login, input.email,loginInfo))

  }

  def getAll(): Future[Iterable[User]] = {
    userRepository.all()
  }
}

