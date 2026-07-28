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

import java.time.LocalDate

class PaymentOnAccountSpec extends SpecBase {

  override val testPaymentOnAccount = PaymentOnAccount(
    paymentReference = Some("3000000000001"),
    amount = BigDecimal("150.00"),
    paymentDate = Some(LocalDate.parse("2026-10-18"))
  )

  val testPaymentOnAccountJson = Json.obj(
    "paymentReference" -> "3000000000001",
    "amount" -> 150.00,
    "paymentDate" -> "2026-10-18"
  )

  "PaymentOnAccountMainTransaction" - {
    "must serialize to JSON correctly" in {
      Json.toJson(testPaymentOnAccount) mustBe testPaymentOnAccountJson
    }

    "must deserialize from JSON correctly" in {
      testPaymentOnAccountJson.validate[PaymentOnAccount] mustBe JsSuccess(testPaymentOnAccount)
    }

    "must handle round-trip serialization" in {
      val json = Json.toJson(testPaymentOnAccount)
      json.as[PaymentOnAccount] mustBe testPaymentOnAccount
    }

    "must handle None values correctly" in {
      val paymentWithNones = PaymentOnAccount(
        paymentReference = None,
        amount = BigDecimal("100.00"),
        paymentDate = None
      )
      val json = Json.toJson(paymentWithNones)
      json.as[PaymentOnAccount] mustBe paymentWithNones
    }
  }
}
