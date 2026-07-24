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

class ClearedPaymentSpec extends SpecBase {

  val testClearedPaymentJson = Json.obj(
    "chargeReference" -> "VPD38270541980",
    "period" -> "September 2026",
    "amountPaid" -> 750.00,
    "clearedDate" -> "2026-10-09"
  )

  "ClearedPayment" - {
    "must serialize to JSON correctly" in {
      Json.toJson(testClearedPayment) mustBe testClearedPaymentJson
    }

    "must deserialize from JSON correctly" in {
      testClearedPaymentJson.validate[ClearedPayment] mustBe JsSuccess(testClearedPayment)
    }

    "must handle round-trip serialization" in {
      val json = Json.toJson(testClearedPayment)
      json.as[ClearedPayment] mustBe testClearedPayment
    }
  }
}
