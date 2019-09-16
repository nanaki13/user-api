package module

import com.google.inject.name.Named
import com.google.inject.{AbstractModule, Provides}
import com.mohiva.play.silhouette.api.crypto.{AuthenticatorEncoder, Crypter, CrypterAuthenticatorEncoder}
import com.mohiva.play.silhouette.api.repositories.AuthInfoRepository
import com.mohiva.play.silhouette.api.services.AuthenticatorService
import com.mohiva.play.silhouette.api.util.{Clock, IDGenerator, PasswordHasherRegistry, PasswordInfo}
import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.crypto.{JcaCrypter, JcaCrypterSettings}
import com.mohiva.play.silhouette.impl.authenticators.{JWTAuthenticator, JWTAuthenticatorService, JWTAuthenticatorSettings}
import com.mohiva.play.silhouette.impl.providers.CredentialsProvider
import com.mohiva.play.silhouette.impl.util.SecureRandomIDGenerator
import com.mohiva.play.silhouette.password.{BCryptPasswordHasher, BCryptSha256PasswordHasher}
import com.mohiva.play.silhouette.persistence.daos.{DelegableAuthInfoDAO, InMemoryAuthInfoDAO}
import com.mohiva.play.silhouette.persistence.repositories.DelegableAuthInfoRepository
import javax.inject._
import net.ceedubs.ficus.Ficus._
import net.ceedubs.ficus.readers.ArbitraryTypeReader._
import net.ceedubs.ficus.readers.ValueReader
import model.User
import net.codingwell.scalaguice.ScalaModule
import play.api.{ConfigLoader, Configuration}
import repository.UserRepository

import scala.concurrent.duration._
import scala.language.postfixOps
import service.UserService
import v1.post._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.reflect.ClassTag
/**
  * Sets up custom components for Play.
  *
  * https://www.playframework.com/documentation/latest/ScalaDependencyInjection
  */
class  Module(environment: play.api.Environment, configuration: Configuration)
    extends AbstractModule
    with ScalaModule {

  override def configure() = {
    bind[PostRepository].to[PostRepositoryImpl].in[Singleton]
    bind[Silhouette[JWTEnv]].to[SilhouetteProvider[JWTEnv]]
  }


  /**
    * Provides the password hasher registry.
    *
    * @return The password hasher registry.
    */
  @Provides
  def providePasswordHasherRegistry(): PasswordHasherRegistry = {
    PasswordHasherRegistry(new BCryptSha256PasswordHasher(), Seq(new BCryptPasswordHasher()))
  }

  /**
    * Provides the credentials provider.
    *
    * @param authInfoRepository The auth info repository implementation.
    * @param passwordHasherRegistry The password hasher registry.
    * @return The credentials provider.
    */
  @Provides
  def provideCredentialsProvider(
                                  authInfoRepository: AuthInfoRepository,
                                  passwordHasherRegistry: PasswordHasherRegistry): CredentialsProvider = {

    new CredentialsProvider(authInfoRepository, passwordHasherRegistry)
  }


  /**
    * Provides the Silhouette environment.
    *
    * @param userService The user service implementation.
    * @param authenticatorService The authentication service implementation.
    * @param eventBus The event bus instance.
    * @return The Silhouette environment.
    */
  @Provides
  def provideEnvironment(
                          userService: UserService,
                          authenticatorService: AuthenticatorService[JWTEnv#A],
                          eventBus: EventBus): Environment[JWTEnv] = {

    Environment[JWTEnv](
      userService,
      authenticatorService,
      Seq(),
      eventBus
    )
  }
  @Provides
  def providePasswordInfoDAO(userRepository: UserRepository): DelegableAuthInfoDAO[PasswordInfo] = {
    userRepository
  }


  /**
    * Provides the auth info repository.
    *

    * @param passwordInfoDAO The implementation of the delegable password auth info DAO.

    * @return The auth info repository instance.
    */
  @Provides
  def provideAuthInfoRepository(
                                 passwordInfoDAO: DelegableAuthInfoDAO[PasswordInfo]): AuthInfoRepository = {

    new DelegableAuthInfoRepository( passwordInfoDAO)
  }
  lazy val idGenerator = new SecureRandomIDGenerator
  @Provides
  def authenticatorService(@Named("authenticator-crypter") crypter: Crypter): AuthenticatorService[JWTAuthenticator] = {

    val settings = JWTAuthenticatorSettings(
      fieldName = configuration.getOptional[String]("silhouette.authenticator.headerName").getOrElse { "X-Auth-Token" },

      issuerClaim = configuration.getOptional[String]("silhouette.authenticator.issueClaim").getOrElse { "play-silhouette" },

  //    authenticatorIdleTimeout = configuration.getOptional("silhouette.authenticator.authenticatorIdleTimeout"), // This feature is disabled by default to prevent the generation of a new JWT on every request
      authenticatorExpiry = configuration.getOptional[FiniteDuration]("silhouette.authenticator.authenticatorExpiry").getOrElse { 12  hours} ,
      sharedSecret = configuration.getOptional[String]("play.http.secret.key").get)
    new JWTAuthenticatorService(
      settings = settings,
      repository = None,
      new CrypterAuthenticatorEncoder(crypter),
      idGenerator = idGenerator,
      clock = Clock())
  }

  /**
    * Provides the crypter for the authenticator.
    *
    * @param configuration The Play configuration.
    * @return The crypter for the authenticator.
    */
  @Provides @Named("authenticator-crypter")
  def provideAuthenticatorCrypter(configuration: Configuration): Crypter = {
    val config = configuration.underlying.as[JcaCrypterSettings]("silhouette.authenticator.crypter")

    new JcaCrypter(config)
  }



}
trait JWTEnv extends Env {
  type I = User
  type A = JWTAuthenticator
}