package controller

import javax.inject.Inject
import play.api.i18n.MessagesApi
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

class UserActionBuilder @Inject()(messagesApi: MessagesApi, playBodyParsers: PlayBodyParsers, implicit val executionContext: ExecutionContext) extends ActionBuilder[UserRequest, AnyContent] {
  override def parser: BodyParser[AnyContent] = playBodyParsers.anyContent

  override def invokeBlock[A](request: Request[A], block: UserRequest[A] => Future[Result]): Future[Result] = {


    block(new UserRequest(request, messagesApi))
  }
}
