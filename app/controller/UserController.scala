package controller

import com.mohiva.play.silhouette.api.actions.{SecuredAction, UnsecuredAction, UserAwareAction}
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.AuthenticatorResult
import com.mohiva.play.silhouette.api.util.{Credentials, PasswordHasherRegistry}
import com.mohiva.play.silhouette.api.{Env, Environment, HandlerResult, LoginEvent, LoginInfo, SignUpEvent, Silhouette, SilhouetteProvider}
import com.mohiva.play.silhouette.impl.authenticators.JWTAuthenticator
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import javax.inject.Inject
import model.{User, UserInputForm}
import play.api.data.Form
import play.api.http.FileMimeTypes
import play.api.i18n.{Langs, MessagesApi}
import play.api.libs.json.{JsValue, Json}
import play.api.mvc.{Result, _}
import repository.UserRepository
import v1.post.RequestMarkerContext
import module.JWTEnv
import security.Token
import security.Token._
import service.UserService
import play.api.libs.json.JodaWrites._
import play.api.libs.json.JodaReads._

import scala.concurrent.{ExecutionContext, Future}

class UserController @Inject()(pcc: UserControllerComponents
                              )(implicit val ec: ExecutionContext) extends UserBaseController(pcc) {


  private val formNewUser: Form[UserInputForm] = {
    import play.api.data.Forms._

    Form(
      mapping(
        "email" -> nonEmptyText,
        "password" -> nonEmptyText,
        "login" -> nonEmptyText,
      )(UserInputForm.apply)(UserInputForm.unapply)
    )
  }

  private val formLoginUser: Form[UserInputForm] = {
    import play.api.data.Forms._

    Form(
      mapping(
        "email" -> nonEmptyText,
        "password" -> nonEmptyText
      )(UserInputForm.apply)(UserInputForm.unapplyShort)
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

  def index: Action[AnyContent] = userAction.async { implicit request =>
    silhouette.SecuredRequestHandler { userAwareRequest =>
      userResourceHandler.getAll().map(users => HandlerResult(Ok, Some(users)))
    }.map {
      case HandlerResult(r, Some(users)) => Ok(Json.toJson(users))
      case HandlerResult(r, None) => Unauthorized
    }
  }

  def create: Action[AnyContent] = userAction.async { implicit request =>
    silhouette.UnsecuredRequestHandler { userAwareRequest =>
      processJsonPostUser()
    }.map {
      case HandlerResult(r, Some(user)) => Ok(Json.toJson(user))
      case HandlerResult(r, None) => Forbidden
    }
  }

  def signIn: Action[AnyContent] = userAction.async { implicit request =>

    processJsonSignIn()


  }


  private def processJsonSignIn[A]()(
    implicit request: UserRequest[A]): Future[Result] = {
    def failure(badForm: Form[UserInputForm]): Future[AuthenticatorResult] = {
      Future.successful(AuthenticatorResult(BadRequest(badForm.errorsAsJson)))
    }

    def success(input: UserInputForm): Future[AuthenticatorResult] = {


      val credentials = Credentials(input.email, input.password)
      pcc.credentialsProvider.authenticate(credentials).flatMap(
        lginIfno => {
          pcc.userService.retrieve(lginIfno).flatMap {
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
      //      userResourceHandler.create(input).map { user =>
      //        HandlerResult(Ok, Some(Json.toJson(user)))
      //        //.withHeaders(LOCATION -> post.link)
      //      }
    }

    formLoginUser.bindFromRequest().fold(failure, success)
  }


  //  def create: Action[AnyContent] = userAction.async { implicit request =>
  //
  //    processJsonPost()
  //  }

  private def processJsonPostUser[A]()(
    implicit request: UserRequest[A]): Future[Result] = {
    def failure(badForm: Form[UserInputForm]): Future[AuthenticatorResult] = {
      Future.successful(HandlerResult(BadRequest, Some(badForm.errorsAsJson)))
    }

    def success(input: UserInputForm): Future[AuthenticatorResult]  = {
      val o: Future[AuthenticatorResult]= userResourceHandler.create(input).map {
        case user => {
          val authInfo = pcc.passwordHasherRegistry.current.hash(input.password)
          val forC: Future[AuthenticatorResult] = for {
//            userToSave <- userService.create(loginInfo, signUp, avatar)
//            user <- userService.save(userToSave)
            authInfo <- pcc.authInfoRepository.save(user.loginInfo, authInfo)
            authenticator <- silhouette.env.authenticatorService.create(user.loginInfo)
            token <- silhouette.env.authenticatorService.init(authenticator)
            result <- silhouette.env.authenticatorService.embed(token,
              Ok(Json.toJson(Token(token = token, expiresOn = authenticator.expirationDateTime))))

          } yield {
            silhouette.env.eventBus.publish(SignUpEvent(user, request))
            silhouette.env.eventBus.publish(LoginEvent(user, request))

            result
          }
          forC


        }.
        case _ => {
          Future.failed(new IdentityNotFoundException("Couldn't create user"))
        }
      }


      }
    }

    HandlerResult(Ok, Some(Json.toJson(user)))
    //.withHeaders(LOCATION -> post.link)

  }
    }

    formNewUser.bindFromRequest().fold(failure, success)
  }


}

case class UserControllerComponents @Inject()(
                                               silhouette: Silhouette[JWTEnv],
                                               actionBuilder: UserActionBuilder,
                                               passwordHasherRegistry: PasswordHasherRegistry,
                                               userRessourcesHandler: UserRessourcesHandler,
                                               userService: UserService,
                                               authInfoRepository: AuthInfoRepository,
                                               credentialsProvider: CredentialsProvider
                                               , parsers: PlayBodyParsers
                                               , messagesApi: MessagesApi
                                               , langs: Langs
                                               , fileMimeTypes: FileMimeTypes
                                               , executionContext: scala.concurrent.ExecutionContext)
  extends ControllerComponents

class UserActionBuilder @Inject()(messagesApi: MessagesApi, playBodyParsers: PlayBodyParsers, implicit val executionContext: ExecutionContext) extends ActionBuilder[UserRequest, AnyContent] {
  override def parser: BodyParser[AnyContent] = playBodyParsers.anyContent

  override def invokeBlock[A](request: Request[A], block: UserRequest[A] => Future[Result]): Future[Result] = {


    block(new UserRequest(request, messagesApi))
  }
}

class UserRessourcesHandler @Inject()(userRpository: UserRepository)(implicit ec: ExecutionContext) {
  def create(input: UserInputForm): Future[User] = {
    userRpository.create(User(input.email, input.password))

  }

  def getAll(): Future[Iterable[User]] = {
    userRpository.all()
  }
}

class UserBaseController @Inject()(pcc: UserControllerComponents)
  extends BaseController
    with RequestMarkerContext {
  override protected def controllerComponents: ControllerComponents = pcc

  def silhouette: Silhouette[JWTEnv] = pcc.silhouette

  def userAction: UserActionBuilder = pcc.actionBuilder

  def userResourceHandler: UserRessourcesHandler = pcc.userRessourcesHandler
}


trait UserRequestHeader
  extends MessagesRequestHeader
    with PreferredMessagesProvider

class UserRequest[A](request: Request[A], val messagesApi: MessagesApi)
  extends WrappedRequest(request)
    with UserRequestHeader
