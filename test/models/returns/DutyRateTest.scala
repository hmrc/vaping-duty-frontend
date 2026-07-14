/*
 * Copyright 2026 HM Revenue & Customs
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

class DutyRateTest extends SpecBase {

  "DutyRate" - {

    "provides the duty rate in Pounds per 10ml for display purposes" in {
      DutyRate(ratePencePer10Ml = 220).dutyRateInPoundsPer10Ml mustBe GBP("2.20")
    }

    "duty calculation" - {
      "calculates the duty in pounds from the volume in ml" in {
        DutyRate(ratePencePer10Ml = 220).calculateDuty(ml("1000")) mustBe GBP("220.00")
      }

      "calculates the duty in pounds from the volume in ml when the duty rate is non-zero in its rightmost digit" in {
        DutyRate(ratePencePer10Ml = 225).calculateDuty(ml("1000")) mustBe GBP("225.00")
      }

      "Rounds down any calculated duty to the nearest penny in the favor of the manufacturer" in {
        DutyRate(ratePencePer10Ml = 333).calculateDuty(ml("1")) mustBe GBP("0.33")
      }
    }
    
    "volume calculation" - {
      "calculates the volume from duty in pounds and pence" in {
        DutyRate(ratePencePer10Ml = 220).volumeForDutyInMl(dutyInPounds = GBP("220.00")) mustBe ml("1000")
      }

      "rounds down the calculated volume to whole millilitres" in {
        DutyRate(ratePencePer10Ml = 20).volumeForDutyInMl(dutyInPounds = GBP("0.03")) mustBe ml("1")
      }
    }
  }

  private def ml(volumeInMl: String) = BigDecimal(volumeInMl)
  private def GBP(poundsAndPence: String) = BigDecimal(poundsAndPence)

}
