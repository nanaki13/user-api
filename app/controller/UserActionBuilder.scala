package controller

import javax.inject.Inject
import play.api.{Logger, MarkerContext}
import play.api.i18n.MessagesApi
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}

class UserActionBuilder @Inject()(messagesApi: MessagesApi, playBodyParsers: PlayBodyParsers, implicit val executionContext: ExecutionContext) extends ActionBuilder[UserRequest, AnyContent] with RequestMarkerContext   {
  override def parser: BodyParser[AnyContent] = playBodyParsers.anyContent
  private val logger = Logger(this.getClass)
  override def invokeBlock[A](request: Request[A], block: UserRequest[A] => Future[Result]): Future[Result] = {
    // Convert to marker context and use request in block
    implicit val markerContext: MarkerContext = requestHeaderToMarkerContext(
      request)
    logger.trace(s"invokeBlock: ")

    block(new UserRequest(request, messagesApi))
  }
}
