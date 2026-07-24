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

class PaymentsResponseSpec extends SpecBase {

  "PaymentsResponse" - {
    "must handle round-trip serialization when all sections are populated" in {
      val json = Json.toJson(testPaymentsResponse)
      json.as[PaymentsResponse] mustBe testPaymentsResponse
    }

    "must handle round-trip serialization when entirely empty" in {
      val json = Json.toJson(PaymentsResponse.empty)
      json.validate[PaymentsResponse] mustBe JsSuccess(PaymentsResponse.empty)
    }

    "empty must have empty sequences for all three sections" in {
      PaymentsResponse.empty.outstanding mustBe Seq.empty
      PaymentsResponse.empty.unallocated mustBe Seq.empty
      PaymentsResponse.empty.cleared mustBe Seq.empty
    }
  }
}
