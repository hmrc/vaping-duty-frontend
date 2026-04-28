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

class ObligationsResponseSpec extends SpecBase {
  val identification: Identification = Identification(
    referenceType = "ZVPD",
    referenceNumber = "GBWK1234567WK",
    incomeSourceType = Some("ITSA")
  )

  val obligationDetails: ObligationDetails = ObligationDetails(
    openOrFulfilledStatus = "O",
    iCFromDate = LocalDate.of(2024, 4, 6),
    iCToDate = LocalDate.of(2024, 7, 5),
    iCDateReceived = Some(LocalDate.of(2024, 6, 15)),
    iCDueDate = LocalDate.of(2024, 8, 5),
    periodKey = "24AB"
  )

  val obligationItem: ObligationItem = ObligationItem(
    identification = Some(identification),
    obligationDetails = obligationDetails
  )

  val obligationItemWithoutIdentification: ObligationItem = obligationItem.copy(identification = None)

  val obligationsResponseMultiple: ObligationsResponse = ObligationsResponse(
    obligation = Seq(obligationItem, obligationItemWithoutIdentification)
  )

  val obligationsResponseSingle: ObligationsResponse = ObligationsResponse(
    obligation = Seq(obligationItem)
  )

  "ObligationsResponse" - {
    val jsonMultiple = """{"obligation":[{"identification":{"referenceType":"ZVPD","referenceNumber":"GBWK1234567WK","incomeSourceType":"ITSA"},"obligationDetails":{"openOrFulfilledStatus":"O","iCFromDate":"2024-04-06","iCToDate":"2024-07-05","iCDateReceived":"2024-06-15","iCDueDate":"2024-08-05","periodKey":"24AB"}},{"obligationDetails":{"openOrFulfilledStatus":"O","iCFromDate":"2024-04-06","iCToDate":"2024-07-05","iCDateReceived":"2024-06-15","iCDueDate":"2024-08-05","periodKey":"24AB"}}]}"""

    val jsonSingle = """{"obligation":[{"identification":{"referenceType":"ZVPD","referenceNumber":"GBWK1234567WK","incomeSourceType":"ITSA"},"obligationDetails":{"openOrFulfilledStatus":"O","iCFromDate":"2024-04-06","iCToDate":"2024-07-05","iCDateReceived":"2024-06-15","iCDueDate":"2024-08-05","periodKey":"24AB"}}]}"""

    "must serialise to json with multiple obligation items" in {
      Json.toJson(obligationsResponseMultiple).toString() mustBe jsonMultiple
    }

    "must deserialise from json with multiple obligation items" in {
      Json.parse(jsonMultiple).as[ObligationsResponse] mustBe obligationsResponseMultiple
    }

    "must serialise to json with single obligation item" in {
      Json.toJson(obligationsResponseSingle).toString() mustBe jsonSingle
    }

    "must deserialise from json with single obligation item" in {
      Json.parse(jsonSingle).as[ObligationsResponse] mustBe obligationsResponseSingle
    }
  }
}