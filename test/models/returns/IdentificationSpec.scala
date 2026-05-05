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

class IdentificationSpec extends SpecBase {
  val identificationWithIncomeSource: Identification = Identification(
    referenceType = "ZVPD",
    referenceNumber = "GBWK1234567WK",
    incomeSourceType = Some("ITSA")
  )

  val identificationWithoutIncomeSource: Identification = identificationWithIncomeSource.copy(incomeSourceType = None)

  "Identification" - {
    val json = """{"referenceType":"ZVPD","referenceNumber":"GBWK1234567WK","incomeSourceType":"ITSA"}"""

    val jsonWithoutIncomeSource = """{"referenceType":"ZVPD","referenceNumber":"GBWK1234567WK"}"""

    "must serialise to json" in {
      Json.toJson(identificationWithIncomeSource).toString() mustBe json
    }

    "must deserialise from json" in {
      Json.parse(json).as[Identification] mustBe identificationWithIncomeSource
    }

    "must serialise to json when incomeSourceType is None" in {
      Json.toJson(identificationWithoutIncomeSource).toString() mustBe jsonWithoutIncomeSource
    }

    "must deserialise from json when incomeSourceType is absent" in {
      Json.parse(jsonWithoutIncomeSource).as[Identification] mustBe identificationWithoutIncomeSource
    }
  }
}