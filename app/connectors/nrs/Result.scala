package connectors.nrs

sealed trait Result

object Result:
  case object Successful extends Result

  case object Unauthorized extends Result

  case object BadRequest extends Result

  case class NetworkError(msg: String, exception: Throwable) extends Result

  case class ServerError(status: Int) extends Result

  case class OtherError(status: Int, body: Option[String]) extends Result
