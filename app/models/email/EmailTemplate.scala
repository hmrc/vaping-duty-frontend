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

enum EmailTemplate(val templateId: JsString) {
  case DutyDueConfirmation extends EmailTemplate(JsString("vpd_duty_due_confirmation"))
  case NilReturnConfirmation extends EmailTemplate(JsString("vpd_nil_return_confirmation"))
  case CreditDueConfirmation extends EmailTemplate(JsString("vpd_credit_due_confirmation"))
}

object EmailTemplate {
  given Writes[EmailTemplate] = Writes {
    case EmailTemplate.DutyDueConfirmation   => EmailTemplate.DutyDueConfirmation.templateId
    case EmailTemplate.NilReturnConfirmation => EmailTemplate.NilReturnConfirmation.templateId
    case EmailTemplate.CreditDueConfirmation => EmailTemplate.CreditDueConfirmation.templateId
  }

  given Reads[EmailTemplate] = Reads {
    case EmailTemplate.DutyDueConfirmation.templateId   => JsSuccess(EmailTemplate.DutyDueConfirmation)
    case EmailTemplate.NilReturnConfirmation.templateId => JsSuccess(EmailTemplate.NilReturnConfirmation)
    case EmailTemplate.CreditDueConfirmation.templateId => JsSuccess(EmailTemplate.CreditDueConfirmation)
    case other                                          => JsError(s"Unknown EmailTemplate: $other")
  }
}
