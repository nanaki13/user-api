package controller

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.services.AuthenticatorResult
import com.mohiva.play.silhouette.api.util.Credentials
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import javax.inject.Inject
import model.{User, UserInputForm}
import play.api.data.Form
import play.api.libs.json.{Format, Json}
import play.api.mvc.{Result, _}
import security.Token._
import security.{Provider, Token}

import scala.concurrent.{ExecutionContext, Future}

class UserController @Inject()(pcc: UserControllerComponents
                              )(implicit val ec: ExecutionContext) extends UserBaseController(pcc) {


  private val formNewUser: Form[UserInputForm] = {
    import play.api.data.Forms._

    Form(
      mapping(
        "email" -> nonEmptyText,
        "login" -> nonEmptyText,
        "password" -> nonEmptyText
      )(UserInputForm.apply)(UserInputForm.unapply)
    )
  }

  private val formLoginUser: Form[UserInputForm] = {
    import play.api.data.Forms._

    Form(
      mapping(
        "email" -> nonEmptyText,
        "password" -> nonEmptyText
      )(UserInputForm.apply)(UserInputForm.mailPasswordOption)
    )
  }

  private val formUpdateUser: Form[UserInputForm] = {
    import play.api.data.Forms._

    Form(
      mapping(
        "email" -> nonEmptyText,
        "login" -> nonEmptyText
      )(UserInputForm.applyMailLogin)(UserInputForm.unapplyMailLogin)
    )
  }

  /**
    * An example for a secured request handler.
    */
  def securedRequestHandler = Action.async { implicit request =>
    silhouette.SecuredRequestHandler { securedRequest =>
      Future.successful(HandlerResult(Ok, Some(securedRequest.identity)))
    }.map {
      case HandlerResult(r, Some(user)) => Ok(Json.toJson(user.login))
      case HandlerResult(r, None) => Unauthorized
    }
  }

  /**
    * An example for an unsecured request handler.
    */
  def unsecuredRequestHandler = Action.async { implicit request =>
    silhouette.UnsecuredRequestHandler { _ =>
      Future.successful(HandlerResult(Ok, Some("some data")))
    }.map {
      case HandlerResult(r, Some(data)) => Ok(data)
      case HandlerResult(r, None) => Forbidden
    }
  }

  /**
    * An example for a user-aware request handler.
    */
  def userAwareRequestHandler = Action.async { implicit request =>
    silhouette.UserAwareRequestHandler { userAwareRequest =>

      Future.successful(HandlerResult(Ok, userAwareRequest.identity))
    }.map {
      case HandlerResult(r, Some(user)) => Ok(Json.toJson(user.login))
      case HandlerResult(r, None) => Unauthorized
    }
  }

  def update: Action[AnyContent] = userAction.async { implicit request =>
    silhouette.SecuredRequestHandler { userAwareRequest =>
      processJsonUpdate().map( HandlerResult(_, Some("yes")))
    }.map {
      case HandlerResult(r, Some(s)) => Ok(Json.toJson(s))
      case HandlerResult(r, None) => Unauthorized
    }
  }

  def index: Action[AnyContent] = userAction.async { implicit request =>
    silhouette.SecuredRequestHandler { userAwareRequest =>
      pcc.userService.getAll().map(users => HandlerResult(Ok, Some(users)))
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



  private def processJsonUpdate[A]()(implicit request: UserRequest[A]): Future[Result] = {
    def failure(badForm: Form[UserInputForm]): Future[Result] = {
      Future.successful(AuthenticatorResult(BadRequest(badForm.errorsAsJson)))
    }

    def success(input: UserInputForm): Future[Result] = {

      Future.successful(AuthenticatorResult(Ok()))

    }
    formUpdateUser.fold(failure,success)
  }

  private def processJsonSignIn[A]()(
    implicit request: UserRequest[A]): Future[Result] = {
    def failure(badForm: Form[UserInputForm]): Future[AuthenticatorResult] = {
      Future.successful(AuthenticatorResult(BadRequest(badForm.errorsAsJson)))
    }

    def success(input: UserInputForm): Future[AuthenticatorResult] = {


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
              Future.failed(new IdentityNotFoundException("Couldn't find user"))
          }
        }
      )

    }

    formLoginUser.bindFromRequest().fold(failure, success)
  }

  case class Message(text: String)

  object Message {
    implicit val format: Format[Message] = Json.format
  }


  private def processJsonPostUser[A]()(

    implicit request: UserRequest[A]): Future[Result] = {

    def success(input: UserInputForm): Future[Result] = {

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

    def failure(input: Form[UserInputForm]): Future[Result] = Future.successful(BadRequest(input.errorsAsJson))

    formNewUser.bindFromRequest().fold(failure, success)
  }


}













