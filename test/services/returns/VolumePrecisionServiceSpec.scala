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

package services.returns

import base.SpecBase

class VolumePrecisionServiceSpec extends SpecBase {

  private val service = new VolumePrecisionService()

  "VolumePrecisionService" - {

    "calculateMaxVolume" - {

      "must calculate correct max volume for a given duty rate" in {
        val dutyRateInPencePerMl = 337 // £3.37 per 10ml = 0.337 pence per ml
        val result = service.calculateMaxVolume(dutyRateInPencePerMl)

        // Max duty = £999,999,999.99
        // Rate = 337 pence per ml = £3.37 per ml
        // Max volume = 999,999,999.99 / 3.37 = 296,735,905.0 ml (rounded down to 1 decimal place)
        result.maxVolumeInMl mustBe BigDecimal("296735905.0")
      }

      "must format the max volume with commas" in {
        val dutyRateInPencePerMl = 337
        val result = service.calculateMaxVolume(dutyRateInPencePerMl)

        result.formattedForDisplay mustBe "296,735,905 ml"
      }

      "must handle different duty rates correctly" in {
        val testCases = Seq(
          (100, BigDecimal("999999999.9"), "999,999,999.9 ml"),  // £1.00 per ml
          (200, BigDecimal("499999999.9"), "499,999,999.9 ml"),  // £2.00 per ml
          (50, BigDecimal("1999999999.9"), "1,999,999,999.9 ml")  // £0.50 per ml
        )

        testCases.foreach { case (rate, expectedMax, expectedFormatted) =>
          val result = service.calculateMaxVolume(rate)
          result.maxVolumeInMl mustBe expectedMax
          result.formattedForDisplay mustBe expectedFormatted
        }
      }

      "must round down to 1 decimal place" in {
        val dutyRateInPencePerMl = 333 // Results in a value with more than 1 decimal place
        val result = service.calculateMaxVolume(dutyRateInPencePerMl)

        // Verify it's rounded down and has at most 1 decimal place
        result.maxVolumeInMl.scale mustBe 1
        result.maxVolumeInMl mustBe BigDecimal("300300300.2")
      }

      "must format values without unnecessary decimal places" in {
        val dutyRateInPencePerMl = 337 // Results in a whole number after rounding
        val result = service.calculateMaxVolume(dutyRateInPencePerMl)

        // Should not show .0 for results where fractional part is zero
        result.formattedForDisplay must not include "."
      }
    }
  }
}