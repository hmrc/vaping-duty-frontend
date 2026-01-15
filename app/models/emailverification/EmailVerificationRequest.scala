/*
 * Copyright 2025 HM Revenue & Customs
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

import play.api.libs.json.{Json, OFormat}

case class EmailVerificationRequest(
  credId: String,
  continueUrl: String,
  origin: String,
  deskproServiceName: String,
  accessibilityStatementUrl: String,
  backUrl: String,
  email: EmailModel,
  labels: Labels,
  lang: String,
  useNewGovUkServiceNavigation: Boolean
)

object EmailVerificationRequest {
  implicit val format: OFormat[EmailVerificationRequest] = Json.format[EmailVerificationRequest]
}

case class LanguageInfo(
  pageTitle: String,
  userFacingServiceName: String
)

object LanguageInfo {
  implicit val format: OFormat[LanguageInfo] = Json.format[LanguageInfo]
}

case class Labels(
  cy: LanguageInfo,
  en: LanguageInfo
)

object Labels {
  implicit val format: OFormat[Labels] = Json.format[Labels]
}

case class EmailModel(
  address: String,
  enterUrl: String
)

object EmailModel {
  implicit val format: OFormat[EmailModel] = Json.format[EmailModel]
}
