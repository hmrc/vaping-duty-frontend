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

package models.payments

import base.SpecBase
import play.api.libs.json.{JsSuccess, Json}

class OutstandingPaymentSpec extends SpecBase {

  val testPayment = OutstandingPayment(
    chargeReference = "VPD38270541977",
    period = "December 2026",
    amountDue = BigDecimal("330000.00"),
    dueDate = "2026-12-15",
    status = "Due"
  )

  val testPaymentJson = Json.obj(
    "chargeReference" -> "VPD38270541977",
    "period" -> "December 2026",
    "amountDue" -> 330000.00,
    "dueDate" -> "2026-12-15",
    "status" -> "Due"
  )

  "OutstandingPayment" - {
    "must serialize to JSON correctly" in {
      Json.toJson(testPayment) mustBe testPaymentJson
    }

    "must deserialize from JSON correctly" in {
      testPaymentJson.validate[OutstandingPayment] mustBe JsSuccess(testPayment)
    }

    "must handle round-trip serialization" in {
      val json = Json.toJson(testPayment)
      json.as[OutstandingPayment] mustBe testPayment
    }
  }
}