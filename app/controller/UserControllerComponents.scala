package controller

import com.mohiva.play.silhouette.api.Silhouette
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.util.PasswordHasherRegistry
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import javax.inject.Inject
import module.JWTEnv
import play.api.http.FileMimeTypes
import play.api.i18n.{Langs, MessagesApi}
import play.api.mvc.{ControllerComponents, PlayBodyParsers}
import service.UserService

case class UserControllerComponents @Inject()(
                                               silhouette: Silhouette[JWTEnv],
                                               actionBuilder: UserActionBuilder,
                                               passwordHasherRegistry: PasswordHasherRegistry,
                                               userService: UserService,
                                               authInfoRepository: AuthInfoRepository,
                                               credentialsProvider: CredentialsProvider
                                               , parsers: PlayBodyParsers
                                               , messagesApi: MessagesApi
                                               , langs: Langs
                                               , fileMimeTypes: FileMimeTypes
                                               , executionContext: scala.concurrent.ExecutionContext,
                                             )
  extends ControllerComponents
