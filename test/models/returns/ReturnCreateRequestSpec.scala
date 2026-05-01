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

class ReturnCreateRequestSpec extends SpecBase {
  val returnCreateRequest: ReturnCreateRequest =
    ReturnCreateRequest(periodKey = "x641",
      vapingProductsProduced = VapingProductsProduced(Seq(), Seq()),
      totalDutyDue = TotalDutyDue(1, 1, 1, 1, 1, 1))

  "ReturnCreateRequest" - {
    val json = """{"periodKey":"x641","vapingProductsProduced":{"nilReturn":[],"regularReturn":[]},"totalDutyDue":{"totalDutyDueVapingProducts":1,"totalDutyOverDeclaration":1,"totalDutyUnderDeclaration":1,"totalDutySpoiltProduct":1,"adjustmentAmount":1,"totalDutyDue":1}}"""

    "must serialise to json" in {
      Json.toJson(returnCreateRequest).toString() mustBe json
    }

    "must deserialise from json" in {
      Json.parse(json).as[ReturnCreateRequest] mustBe returnCreateRequest
    }
  }
}
