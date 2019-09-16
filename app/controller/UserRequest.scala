package controller

import play.api.i18n.MessagesApi
import play.api.mvc.{Request, WrappedRequest}

class UserRequest[A](request: Request[A], val messagesApi: MessagesApi)
  extends WrappedRequest(request)
    with UserRequestHeader
