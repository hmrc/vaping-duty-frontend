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

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import play.api.libs.json.{JsError, JsString, JsSuccess, Json}

class EmailTemplateSpec extends AnyFreeSpec with Matchers {

  "EmailTemplate" - {

    "must write the correct templateId string for each case" in {
      Json.toJson[EmailTemplate](EmailTemplate.DutyDueConfirmation) mustBe JsString("vpd_duty_due_confirmation")
      Json.toJson[EmailTemplate](EmailTemplate.NilReturnConfirmation) mustBe JsString("vpd_nil_return_confirmation")
      Json.toJson[EmailTemplate](EmailTemplate.CreditDueConfirmation) mustBe JsString("vpd_credit_due_confirmation")
    }

    "must read the correct case for each templateId string" in {
      Json.fromJson[EmailTemplate](JsString("vpd_duty_due_confirmation")) mustBe JsSuccess(EmailTemplate.DutyDueConfirmation)
      Json.fromJson[EmailTemplate](JsString("vpd_nil_return_confirmation")) mustBe JsSuccess(EmailTemplate.NilReturnConfirmation)
      Json.fromJson[EmailTemplate](JsString("vpd_credit_due_confirmation")) mustBe JsSuccess(EmailTemplate.CreditDueConfirmation)
    }

    "must fail to read an unknown templateId string" in {
      Json.fromJson[EmailTemplate](JsString("unknown_template")) mustBe a[JsError]
    }
  }
}
