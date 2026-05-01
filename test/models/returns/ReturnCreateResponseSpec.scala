/*
 * Copyright 2024 HM Revenue & Customs
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

package models.returns

import base.SpecBase
import play.api.libs.json.Json

import java.time.Instant

class ReturnCreateResponseSpec extends SpecBase {
  val returnCreateResponse: ReturnCreateResponse =
    ReturnCreateResponse(success = ReturnSubmittedResponse(
      processingDate = Instant.parse("2024-01-01T00:00:00Z"),
      vpdReferenceNumber = "VPD1234567890",
      submissionID = Some("SUbID"),
      chargeReference = Some("CR123"),
      amount = BigDecimal(1.0),
     paymentDueDate = Some(java.time.LocalDate.parse("2024-01-31"))))


  "ReturnCreateResponse" - {
    val json = """{"success":{"processingDate":"2024-01-01T00:00:00Z","vpdReferenceNumber":"VPD1234567890","submissionID":"SUbID","chargeReference":"CR123","amount":1,"paymentDueDate":"2024-01-31"}}"""

    "must serialise to json" in {
      Json.toJson(returnCreateResponse).toString() mustBe json
    }

    "must deserialise from json" in {
      Json.parse(json).as[ReturnCreateResponse] mustBe returnCreateResponse
    }
  }
}
