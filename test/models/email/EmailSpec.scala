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
import play.api.libs.json.Json

class EmailSpec extends AnyFreeSpec with Matchers {

  "Email" - {

    "must serialise a DutyDueEmail with its templateId and parameters" in {
      val email = DutyDueEmail(
        to = List("test@example.com"),
        parameters = DutyDueEmailParameters(
          recipientName = "John Smith",
          returnPeriod = "October 2026",
          submissionDate = "3 November 2026",
          chargeReference = "VPD38270541977",
          amountDue = "£1,234.50",
          paymentDueDate = "15 November 2026"
        )
      )

      val json = Json.toJson[Email](email)

      (json \ "to").as[List[String]] mustBe List("test@example.com")
      (json \ "templateId").as[String] mustBe "vpd_duty_due_confirmation"
      (json \ "parameters" \ "chargeReference").as[String] mustBe "VPD38270541977"
      (json \ "parameters" \ "amountDue").as[String] mustBe "£1,234.50"
    }

    "must serialise a NilReturnEmail with its templateId and parameters" in {
      val email = NilReturnEmail(
        to = List("test@example.com"),
        parameters = NilReturnEmailParameters(
          recipientName = "John Smith",
          returnPeriod = "October 2026",
          submissionDate = "3 November 2026"
        )
      )

      val json = Json.toJson[Email](email)

      (json \ "templateId").as[String] mustBe "vpd_nil_return_confirmation"
      (json \ "parameters" \ "recipientName").as[String] mustBe "John Smith"
    }

    "must serialise a CreditDueEmail with its templateId and parameters" in {
      val email = CreditDueEmail(
        to = List("test@example.com"),
        parameters = CreditDueEmailParameters(
          recipientName = "John Smith",
          returnPeriod = "October 2026",
          submissionDate = "3 November 2026",
          creditAmount = "£50"
        )
      )

      val json = Json.toJson[Email](email)

      (json \ "templateId").as[String] mustBe "vpd_credit_due_confirmation"
      (json \ "parameters" \ "creditAmount").as[String] mustBe "£50"
    }
  }
}
