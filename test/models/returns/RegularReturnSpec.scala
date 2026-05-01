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

class RegularReturnSpec extends SpecBase {
  val regularReturn: RegularReturn =
    RegularReturn(taxType = "x641",
      dutyRate = BigDecimal(0.20),
      amountProducedLiquid = BigDecimal(100),
      dutyDue = BigDecimal(30))

  "RegularReturn" - {
    val json = """{"taxType":"x641","dutyRate":0.2,"amountProducedLiquid":100,"dutyDue":30}"""

    "must serialise to json" in {
      Json.toJson(regularReturn).toString() mustBe json
    }

    "must deserialise from json" in {
      Json.parse(json).as[RegularReturn] mustBe regularReturn
    }
  }
}
