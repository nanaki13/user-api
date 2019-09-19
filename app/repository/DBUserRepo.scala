package repository

import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.api.util.{PasswordHasherRegistry, PasswordInfo}
import com.mohiva.play.silhouette.impl.exceptions.IdentityNotFoundException
import com.mohiva.play.silhouette.persistence.daos.DelegableAuthInfoDAO
import javax.inject.Inject
import model.{Role, User}
import model._
import play.api.Configuration
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import security.Provider
import slick.ast.{ScalaBaseType, Type, TypedType}
import slick.dbio.DBIOAction
import slick.jdbc.JdbcProfile
import slick.jdbc.meta.MTable
import slick.lifted
import slick.lifted.QueryBase

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.reflect.ClassTag

class DBUserRepo @Inject()(protected val dbConfigProvider: DatabaseConfigProvider, init: InitRepoBase)(implicit ec: ExecutionContext)
  extends DelegableAuthInfoDAO[PasswordInfo] with UserRepository with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._
  implicit val roleBridge = MappedColumnType.base[Role, String](
    {_.role},{Role(_)}
  )
  type UserType = (Int, String, String, Option[String], Option[String], Role)
  class Users(tag: Tag) extends Table[User](tag, "USER") {

    def id = column[Int]("ID", O.PrimaryKey, O.AutoInc)

    def login = column[String]("LOGIN")

    def password = column[Option[String]]("PASSWORD")

    def hasher = column[Option[String]]("HASHER")

    def mail = column[String]("MAIL")

    def role = column[Role]("ROLE")

    def * = (id, login, mail, password, hasher, role).shaped <> (UserDB.tupled , UserDB.unapply )
  }
  object UserDB {


    def unapply(u: User) =  Some(u.id, u.login, u.email, u.passwordInfo.map(_.password), u.passwordInfo.map(_.hasher), u.role)

    def tupled(t: UserType): User = User(t._1, t._2, t._3, LoginInfo(Provider.name, t._3), t._6, t._4.map(PasswordInfo( t._5.get,_)))
  }



  lazy val userTable = lifted.TableQuery[Users]


  def byIdQuery(user: User) = for {c <- userTable if c.id === user.id} yield c

  def byIdQuery(id: Int) = for {c <- userTable if c.id === id} yield c

  def byEmailQuery(user: User): QueryBase[Seq[User]] = byLoginQuery(user.loginInfo)

  def byLoginQuery(loginInfo: LoginInfo) = for {c <- userTable if c.mail === loginInfo.providerKey} yield c

  override def delete(idInt: Int): Future[Boolean] = {
    val q = userTable.filter(_.id === idInt)
    val action = q.delete
    val affectedRowsCount: Future[Int] = db.run(action)
    affectedRowsCount.map(cnt => cnt == 1)

  }

  // override def init: Init =

  override def removeUser(loginInfo: LoginInfo): Future[Boolean] = {
    val action = byLoginQuery(loginInfo).delete
    val affectedRowsCount: Future[Int] = db.run(action)
    affectedRowsCount.map(cnt => cnt == 1)

  }

  override def findByLoginInfo(loginInfo: LoginInfo): Future[Option[User]] = {
    val action = byLoginQuery(loginInfo).result.headOption
    db.run(action).map{
       case Some(user: User) => Some(user)
       case None => None
    }
  }

  override def create(user: User): Future[User] = {
    val action = userTable += user
    for {
      newUser <- db.run(action).map { cnt => {
        if (cnt == 1) {
          findByLoginInfo(user.loginInfo)
        } else {
          Future.failed(new IdentityNotFoundException("cant create  : " + user.loginInfo))
        }

      }
      }
      unpack <- newUser map {
        case Some(u) => u
        case _ => throw new IdentityNotFoundException("cant create  : " + user.loginInfo)
      }
    } yield {
      unpack
    }
  }


  override def update(user: User): Future[Option[User]] = {
    val q = byIdQuery(user)
    val action = q.update(user)
    db.run(action).flatMap(cnt => {
      if (cnt == 1) {
        find(user.id)
      } else {
        Future.successful(Option.empty[User])
      }
    })
  }

  override def find(id: Int): Future[Option[User]] = db.run(byIdQuery(id).result.headOption)

  override def all(): Future[Iterable[User]] = db.run(userTable.result)

  /**
    * Saves the password info.
    *
    * @param loginInfo The login info for which the auth info should be saved.
    * @param authInfo  The password info to save.
    * @return The saved password info or None if the password info couldn't be saved.
    */
  override def save(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = {
    update(loginInfo, authInfo)
  }

  /**
    * Finds the password info which is linked with the specified login info.
    *
    * @param loginInfo The linked login info.
    * @return The retrieved password info or None if no password info could be retrieved for the given login info.
    */
  override def find(loginInfo: LoginInfo): Future[Option[PasswordInfo]] = findByLoginInfo(loginInfo).map {
    case Some(u) => u.passwordInfo
    case _ => None
  }

  override def add(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = update(loginInfo, authInfo)

  override def update(loginInfo: LoginInfo, authInfo: PasswordInfo): Future[PasswordInfo] = {

    val qUpdate = for {
      user <- userTable if user.mail === loginInfo.providerKey
    } yield {
      (user.password, user.hasher)
    }
    db.run(qUpdate.update(Some(authInfo.password), Some(authInfo.hasher)).map(_ => authInfo))

  }


  override def remove(loginInfo: LoginInfo): Future[Unit] = ???

  val tables = List(userTable)

   def createTables = {
     val existing = db.run(MTable.getTables)
     val f = existing.flatMap( v => {
       val names = v.map(mt => mt.name.name)
       val createIfNotExist = tables.filter( table =>
         (!names.contains(table.baseTableRow.tableName))).map(_.schema.create)
       db.run(DBIO.sequence(createIfNotExist))
     })

      f
   }
  val res = Await.result(init.init(this),Duration.Inf)



}



case class InitDB @Inject()( hasher: PasswordHasherRegistry,
                                  executionContext: UserExecutionContext,
                                  configuration: Configuration) extends InitRepoBase {
  override def init(implicit userRepository: UserRepository): Future[Boolean] ={
    Await.result(userRepository.asInstanceOf[DBUserRepo].createTables,Duration.Inf)
    createAdmin

  }

}