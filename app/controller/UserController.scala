package controller

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.services.AuthenticatorResult
import com.mohiva.play.silhouette.api.util.{Credentials, PasswordInfo}
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import javax.imageio.spi.ImageReaderWriterSpi
import javax.inject.Inject
import model.{Role, User, UserInputForm, UserUpdateForm}
import model.User._
import play.api.data.Form
import play.api.http.Writeable
import play.api.i18n.MessagesProvider
import play.api.libs.json.{Format, JsValue, Json}
import play.api.mvc.{Action, Result, _}
import security.Token._
import security.{Provider, Token}
import play.api.data.Forms._
import scala.concurrent.{ExecutionContext, Future}

class UserController @Inject()(pcc: UserControllerComponents
                              )(implicit val ec: ExecutionContext) extends UserBaseController(pcc) {


  private val formNewUser: Form[UserInputForm] = {
    Form(
      mapping(
        "email" -> email,
        "pseudonym" -> nonEmptyText,
        "password" -> nonEmptyText,
      )(UserInputForm.apply)(UserInputForm.unapply)
    )
  }

  private val formLoginUser: Form[UserInputForm] = {
    Form(
      mapping(
        "email" -> nonEmptyText,
        "password" -> nonEmptyText
      )(UserInputForm.apply)(UserInputForm.mailPasswordOption)
    )
  }


  private val formUpdateUser: Form[UserUpdateForm] = {
    Form(
      mapping(
        "id" -> number,
        "email" -> optional(nonEmptyText),
        "pseudonym" -> optional(nonEmptyText),
        "password" -> optional(nonEmptyText)
      )(UserUpdateForm.apply)(UserUpdateForm.unapply)
    )
  }


  def index: Action[AnyContent] = userAction.async { implicit request =>
    silhouette.SecuredRequestHandler { userAwareRequest =>
      val role = userAwareRequest.identity.role
      val authenticatedUser: User = userAwareRequest.identity
      if (role.canCreate(Role.USER)) {
        for {
          result <- pcc.userService.getAll().map { iterUser =>
            iterUser.filter(uLoop => authenticatedUser.canRead(uLoop))
          }

        } yield {
          HandlerResult(Ok, Some(result))
        }
      } else {
        Future.successful(HandlerResult(Unauthorized, None))
      }

    }.map {
      case HandlerResult(r, Some(users)) => Ok(Json.toJson(users))
      case HandlerResult(r, None) => Unauthorized
    }
  }

  def signUp: Action[AnyContent] = userAction.async { implicit request =>
    processJsonPostUser()
  }


  def signIn: Action[AnyContent] = userAction.async { implicit request =>
    processJsonSignIn()
  }

  def admin(implicit role: Role) = role == Role.ADMIN

  def update: Action[AnyContent] = userAction.async { implicit request =>
    silhouette.SecuredRequestHandler { userAwareRequest =>
      val pro = processJsonUpdate(userAwareRequest.identity, request)
      pro.map(e => {
        HandlerResult(e._1, Some(e._2))
      })

    }.map {
      case HandlerResult(s, Some(user: User)) => Status(s.header.status)(Json.toJson(user))
      case HandlerResult(s, Some(js: JsValue)) => Status(s.header.status)(js)
      case HandlerResult(r, None) => Status(r.header.status)(Json.toJson(Message("none result from process", r.header.status)))
      case e => InternalServerError(Json.toJson(Message("Unexpected response")))
    }
  }

  def delete(id: Int) = userAction.async { implicit request =>
    silhouette.SecuredRequestHandler { userAwareRequest =>
      val authUser = userAwareRequest.identity
      if (authUser.role.canDelete()) {
        pcc.userService.find(id).flatMap {
          case Some(u: User) => {
            if (authUser.canDelete(u)) {
              pcc.userService.delete(id)
            } else {
              Future.successful(Unauthorized)
            }
          }
          case _ => Future.successful(None)
        }.map { e =>
          e match {
            case true => HandlerResult(Ok, None)
            case false => HandlerResult(NoContent, None)
            case None => HandlerResult(NoContent, None)
            case Unauthorized => HandlerResult(Unauthorized, None)
          }

        }
      } else {
        Future.successful(HandlerResult(Unauthorized))
      }
    }.map {
      case HandlerResult(Ok, _) => Ok
      case HandlerResult(NoContent, _) => NoContent
      case HandlerResult(r, None) => Status(r.header.status)(Json.toJson(Message("none result from process", r.header.status)))
      case e => InternalServerError(Json.toJson(Message("Unexpected response")))
    }
  }

   def getSelf = userAction.async { implicit request =>
    silhouette.SecuredRequestHandler { userAwareRequest =>
      val authUser = userAwareRequest.identity
      
        pcc.userService.find(authUser.id).map {
          case Some(u: User) => HandlerResult(Ok, Some(u))
          case _ => HandlerResult(NotFound, None)
        }
     
    }.map {
      case HandlerResult(Ok,Some(data)) => Ok(Json.toJson(data))
      case HandlerResult(NoContent, _) => NoContent
      case HandlerResult(r, None) => Status(r.header.status)(Json.toJson(Message("none result from process", r.header.status)))
      case e => InternalServerError(Json.toJson(Message("Unexpected response")))
    }
  }

  def get(id: Int) = userAction.async { implicit request =>
    silhouette.SecuredRequestHandler { userAwareRequest =>
      val authUser = userAwareRequest.identity
      if (authUser.role.canRead() && authUser.id == id || authUser.role == Role.ADMIN) {
        pcc.userService.find(id).flatMap {
          case Some(u: User) => {
            if (authUser.canRead(u)) {
              pcc.userService.find(id)
            } else {
              Future.successful(Unauthorized)
            }
          }
          case _ => Future.successful(None)
        }.map { e =>
          e match {
            case Some(u : User) => HandlerResult(Ok, Some(Json.toJson(u)))
            case None => HandlerResult(NoContent, None)
            case Unauthorized => HandlerResult(Unauthorized, None)
          }

        }
      } else {
        Future.successful(HandlerResult(Unauthorized))
      }
    }.map {
      case HandlerResult(Ok,Some(data)) => Ok(data)
      case HandlerResult(NoContent, _) => NoContent
      case HandlerResult(r, None) => Status(r.header.status)(Json.toJson(Message("none result from process", r.header.status)))
      case e => InternalServerError(Json.toJson(Message("Unexpected response")))
    }
  }

  private def processJsonUpdate[A](implicit authUser: User, request: UserRequest[A]): Future[(Status, Any)] = {
    implicit val role = authUser.role

    def failure(badForm: Form[UserUpdateForm]): Future[(Status, Any)] = {
      Future.successful(BadRequest, badForm.errorsAsJson)
    }

    def success(input: UserUpdateForm): Future[(Status, Any)] = {
      val userId = authUser.id
      if (input.id == userId || authUser.role > Role.USER) {
        for {
          findedUser <- pcc.userService.find(input.id)
          ret <- {
            findedUser match {
              case Some(u) => if (authUser.canUpdate(u)) {
                updateUser(input)(u)
              } else {
                Future.successful(Unauthorized, Message("You can't"))
              }
              case None => Future.successful(Status(NOT_MODIFIED), AnyContentAsEmpty)
            }
          }
        } yield {
          ret
        }

      } else {
        Future.successful(Unauthorized, Message("You can't"))
      }
    }

    formUpdateUser.bindFromRequest().fold(failure, success)
  }

  def updateUser(input: UserUpdateForm)(implicit user: User): Future[(Status, Any)] = {
    val upddatOrGetPass: Future[Option[PasswordInfo]] = input.password match {
      case Some(pass) => {
        val newHashPass = pcc.passwordHasherRegistry.current.hash(pass)
        pcc.authInfoRepository.update(user.loginInfo, newHashPass).map(Some(_))
      }
      case None => Future.successful(user.passwordInfo)
    }

    for {
      pInfo <- upddatOrGetPass
      userUpdated <- pcc.userService.update(
        user.copy(
          pseudonym = input.pseudonym.getOrElse(user.pseudonym),
          email = input.email.getOrElse(user.email),
          loginInfo = input.pseudonym.map(LoginInfo(Provider.name, _)).getOrElse(user.loginInfo),
          passwordInfo = pInfo
        )).map { e =>
        if (input.pseudonym.isEmpty && input.email.get != user.email) {
          pcc.authInfoRepository.remove(user.loginInfo)
          pcc.authInfoRepository.save(e.get.loginInfo, pInfo.get)

        }
        e
      }
    } yield {
      userUpdated match {
        case Some(u) => (Ok, u)
        case None => (Status(NOT_MODIFIED), "")
      }

    }


  }


  private def processJsonSignIn[A]()(
    implicit request: UserRequest[A]): Future[Result] = {
    def failure(badForm: Form[UserInputForm]): Future[Result] = {
      Future.successful(AuthenticatorResult(BadRequest(badForm.errorsAsJson)))
    }

    def success(input: UserInputForm): Future[Result] = {

      val credentials = Credentials(input.email, input.password)

      pcc.credentialsProvider.authenticate(credentials).flatMap(
        loginInfo => {
          pcc.userService.retrieve(loginInfo).flatMap {

            case Some(user) => silhouette.env.authenticatorService.create(user.loginInfo).flatMap(authenticator => {
              silhouette.env.eventBus.publish(LoginEvent(user, request))
              silhouette.env.authenticatorService.init(authenticator).flatMap { token =>
                silhouette.env.authenticatorService.embed(token,
                  Ok(Json.toJson(Token(token = token, expiresOn = authenticator.expirationDateTime)))
                )
              }

            })
            case None =>
              Future.successful(NotFound(Json.toJson(Message("cant authenticate"))))
          }
        }
      )
    }

    try {
      formLoginUser.bindFromRequest().fold(failure, success)
    } catch {
      case e: IdentityNotFoundException => Future.successful(NotFound(Json.toJson(Message("cant authenticate"))))
    }
  }


  private def processJsonPostUser[A]()(

    implicit request: UserRequest[A]): Future[Result] = {

    def success(input: UserInputForm): Future[Result] = {

      println(input)
      val loginInfo = LoginInfo(Provider.name, input.email)

      pcc.userService.retrieve(loginInfo).flatMap {
        case Some(u) => Future.successful(AuthenticatorResult(Conflict(Json.toJson(Message("User already exists")))))
        case None =>
          val authInfo = pcc.passwordHasherRegistry.current.hash(input.password)
          val res: Future[Result] = for {
            user <- pcc.userService.create(loginInfo, input)
            a <- pcc.authInfoRepository.save(loginInfo, authInfo)
          } yield {
            silhouette.env.eventBus.publish(SignUpEvent(user, request))

            Ok(Json.toJson(user))
          }

          res
      }
    }

    def failure(input: Form[UserInputForm]): Future[Result] = {
      print(input)
      Future.successful(BadRequest(input.errorsAsJson))
    }

    try {
      formNewUser.bindFromRequest().fold(failure, success)
    } catch {
      case e: IdentityNotFoundException => Future.successful(NotFound(Json.toJson(Message("cant authenticate"))))
    }

  }


}


case class Message(text: String, statusHttp: Option[Int] = None)

object Message {
  def apply(text: String, statusHttp: Int): Message = {
    Message(text, Some(statusHttp))
  }

  implicit val format: Format[Message] = Json.format
}











