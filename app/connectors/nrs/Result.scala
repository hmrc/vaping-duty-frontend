/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package connectors.nrs

sealed trait Result

object Result:
  case object Successful extends Result

  case object Unauthorized extends Result

  case object BadRequest extends Result

  case class NetworkError(msg: String, exception: Throwable) extends Result

  case class ServerError(status: Int) extends Result

  case class OtherError(status: Int, body: Option[String]) extends Result
