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

package models.nrs

import play.api.libs.json.*

sealed trait CredentialRole

object CredentialRole {
  case object Admin extends CredentialRole
  case object Assistant extends CredentialRole
  case object User extends CredentialRole

  implicit val format: Format[CredentialRole] = new Format[CredentialRole] {
    def reads(json: JsValue): JsResult[CredentialRole] = json.as[String] match {
      case "Admin" => JsSuccess(Admin)
      case "Assistant" => JsSuccess(Assistant)
      case "User" => JsSuccess(User)
      case other => JsError(s"Unknown credential role: $other")
    }

    def writes(role: CredentialRole): JsValue = JsString(role.toString)
  }
}