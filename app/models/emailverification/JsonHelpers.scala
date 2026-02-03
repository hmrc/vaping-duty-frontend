/*
 * Copyright 2024 HM Revenue & Customs
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

package models.emailverification

import play.api.libs.json.*

object JsonHelpers {
  implicit val booleanReads: Reads[Boolean] = {
    case JsString("0") => JsSuccess(false)
    case JsString("1") => JsSuccess(true)
    case s: JsString   => JsError(s"$s is not a valid Boolean")
    case v             => JsError(s"got $v was expecting a string representing a Boolean")
  }

  implicit val booleanWrites: Writes[Boolean] = {
    case false => JsString("0")
    case true  => JsString("1")
  }

  implicit val booleanFormat: Format[Boolean] = Format[Boolean](booleanReads, booleanWrites)
}
