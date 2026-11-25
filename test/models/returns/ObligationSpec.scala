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
import models.obligations.Obligation
import play.api.libs.json.Json


class ObligationSpec extends SpecBase {
  "Obligation" - {
    val fulfilledObligationJson = """{"openOrFulfilledStatus":"F","iCFromDate":"2024-02-01","iCToDate":"2024-02-29","iCDateReceived":"2024-03-08","iCDueDate":"2024-03-07","periodKey":"24KB"}"""

    val openObligationJson = """{"openOrFulfilledStatus":"O","iCFromDate":"2024-02-01","iCToDate":"2024-02-29","iCDueDate":"2024-03-07","periodKey":"24KB"}"""

    "must serialise to json when obligation is fulfilled" in {
      Json.toJson(fulfilledObligation(february2024)).toString() mustBe fulfilledObligationJson
    }

    "must deserialise from json when obligation is fulfilled" in {
      Json.parse(fulfilledObligationJson).as[Obligation] mustBe fulfilledObligation(february2024)
    }

    "must serialise to json when obligation is open" in {
      Json.toJson(openObligation(february2024)).toString() mustBe openObligationJson
    }

    "must deserialise from json when obligation is open" in {
      Json.parse(openObligationJson).as[Obligation] mustBe openObligation(february2024)
    }
  }
}