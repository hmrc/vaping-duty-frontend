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

import java.time.LocalDate

class ObligationDetailsSpec extends SpecBase {
  val obligationDetails: ObligationDetails = ObligationDetails(
    openOrFulfilledStatus = ObligationStatus.O.toString,
    iCFromDate = LocalDate.of(2024, 4, 6),
    iCToDate = LocalDate.of(2024, 7, 5),
    iCDateReceived = Some(LocalDate.of(2024, 6, 15)),
    iCDueDate = LocalDate.of(2024, 8, 5),
    periodKey = "24AB"
  )

  val obligationDetailsWithoutDateReceived: ObligationDetails = obligationDetails.copy(iCDateReceived = None)

  "ObligationDetails" - {
    val json = """{"openOrFulfilledStatus":"O","iCFromDate":"2024-04-06","iCToDate":"2024-07-05","iCDateReceived":"2024-06-15","iCDueDate":"2024-08-05","periodKey":"24AB"}"""

    val jsonWithoutDateReceived = """{"openOrFulfilledStatus":"O","iCFromDate":"2024-04-06","iCToDate":"2024-07-05","iCDueDate":"2024-08-05","periodKey":"24AB"}"""

    "must serialise to json" in {
      Json.toJson(obligationDetails).toString() mustBe json
    }

    "must deserialise from json" in {
      Json.parse(json).as[ObligationDetails] mustBe obligationDetails
    }

    "must serialise to json when iCDateReceived is None" in {
      Json.toJson(obligationDetailsWithoutDateReceived).toString() mustBe jsonWithoutDateReceived
    }

    "must deserialise from json when iCDateReceived is absent" in {
      Json.parse(jsonWithoutDateReceived).as[ObligationDetails] mustBe obligationDetailsWithoutDateReceived
    }
  }
}