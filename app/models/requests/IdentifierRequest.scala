package models.requests

import play.api.mvc.{Request, WrappedRequest}

case class IdentifierRequest[A](request: Request[A], groupId: String, userId: String)
  extends WrappedRequest[A](request)