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

package models.email

import play.api.libs.json.*

sealed trait Email { def to: List[String] }

final case class DutyDueEmail(
  to: List[String],
  templateId: EmailTemplate = EmailTemplate.DutyDueConfirmation,
  parameters: DutyDueEmailParameters
) extends Email

final case class NilReturnEmail(
  to: List[String],
  templateId: EmailTemplate = EmailTemplate.NilReturnConfirmation,
  parameters: NilReturnEmailParameters
) extends Email

final case class CreditDueEmail(
  to: List[String],
  templateId: EmailTemplate = EmailTemplate.CreditDueConfirmation,
  parameters: CreditDueEmailParameters
) extends Email

final case class DutyDueEmailParameters(
  recipientName: String,
  returnPeriod: String,
  submissionDate: String,
  chargeReference: String,
  amountDue: String,
  paymentDueDate: String
)

final case class NilReturnEmailParameters(
  recipientName: String,
  returnPeriod: String,
  submissionDate: String
)

final case class CreditDueEmailParameters(
  recipientName: String,
  returnPeriod: String,
  submissionDate: String,
  creditAmount: String
)

object Email {
  given OWrites[Email] = OWrites {
    case dutyDueEmail: DutyDueEmail     => Json.toJson(dutyDueEmail).as[JsObject]
    case nilReturnEmail: NilReturnEmail => Json.toJson(nilReturnEmail).as[JsObject]
    case creditDueEmail: CreditDueEmail => Json.toJson(creditDueEmail).as[JsObject]
  }
}

object DutyDueEmail   { given OFormat[DutyDueEmail]   = Json.format[DutyDueEmail] }
object NilReturnEmail { given OFormat[NilReturnEmail] = Json.format[NilReturnEmail] }
object CreditDueEmail { given OFormat[CreditDueEmail] = Json.format[CreditDueEmail] }

object DutyDueEmailParameters   { given OFormat[DutyDueEmailParameters]   = Json.format[DutyDueEmailParameters] }
object NilReturnEmailParameters { given OFormat[NilReturnEmailParameters] = Json.format[NilReturnEmailParameters] }
object CreditDueEmailParameters { given OFormat[CreditDueEmailParameters] = Json.format[CreditDueEmailParameters] }
