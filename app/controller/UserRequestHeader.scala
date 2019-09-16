package controller

import play.api.mvc.{MessagesRequestHeader, PreferredMessagesProvider}

trait UserRequestHeader
  extends MessagesRequestHeader
    with PreferredMessagesProvider
