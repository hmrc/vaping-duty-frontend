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
import models.returns.DutyRate

class VolumePrecisionServiceSpec extends SpecBase {

  private val service = new VolumePrecisionService()

  "normalizeMaxVolume" - {

    "must normalize large numbers by taking first two digits and replacing rest with zeros" in {
      service.normalizeMaxVolume(BigDecimal("454545454545")) mustBe BigDecimal("450000000000")
      service.normalizeMaxVolume(BigDecimal("296735905")) mustBe BigDecimal("290000000")
      service.normalizeMaxVolume(BigDecimal("12345678")) mustBe BigDecimal("12000000")
      service.normalizeMaxVolume(BigDecimal("12345")) mustBe BigDecimal("12000")
    }

    "must normalize three-digit numbers" in {
      service.normalizeMaxVolume(BigDecimal("987")) mustBe BigDecimal("980")
      service.normalizeMaxVolume(BigDecimal("123")) mustBe BigDecimal("120")
      service.normalizeMaxVolume(BigDecimal("100")) mustBe BigDecimal("100")
    }

    "must keep two-digit numbers unchanged" in {
      service.normalizeMaxVolume(BigDecimal("87")) mustBe BigDecimal("87")
      service.normalizeMaxVolume(BigDecimal("45")) mustBe BigDecimal("45")
      service.normalizeMaxVolume(BigDecimal("10")) mustBe BigDecimal("10")
    }

    "must keep single-digit numbers unchanged" in {
      service.normalizeMaxVolume(BigDecimal("7")) mustBe BigDecimal("7")
      service.normalizeMaxVolume(BigDecimal("1")) mustBe BigDecimal("1")
    }

    "must handle zero" in {
      service.normalizeMaxVolume(BigDecimal("0")) mustBe BigDecimal("0")
    }

    "must handle negative numbers" in {
      service.normalizeMaxVolume(BigDecimal("-100")) mustBe BigDecimal("-100")
    }
  }

  "calculateMaxVolume" - {

    "must calculate and normalize max volume for a given duty rate" in {
      // Duty rate: £3.37 per ml (3370 pence per 10 ml)
      // Max duty: £99,999,999,999.99
      // Actual max volume: 99,999,999,999.99 / 3.37 = 29,673,590,504.45... → 29,673,590,504 (rounded down)
      // Normalized: 29,000,000,000
      val result = service.calculateMaxVolume(DutyRate(3370))
      
      result.maxVolumeInMl mustBe BigDecimal("29000000000")
      result.formattedForDisplay mustBe "29,000,000,000 ml"
    }

    "must calculate and normalize max volume for a different duty rate" in {
      // Duty rate: £0.22 per ml (220 pence per 10 ml)
      // Max duty: £99,999,999,999.99
      // Actual max volume: 99,999,999,999.99 / 0.22 = 454,545,454,545.40... → 454,545,454,545 (rounded down)
      // Normalized: 450,000,000,000
      val result = service.calculateMaxVolume(DutyRate(220))
      
      result.maxVolumeInMl mustBe BigDecimal("450000000000")
      result.formattedForDisplay mustBe "450,000,000,000 ml"
    }

    "must calculate and normalize max volume for a high duty rate" in {
      // Duty rate: £100 per ml (100,000 pence per 10 ml)
      // Max duty: £99,999,999,999.99
      // Actual max volume: 99,999,999,999.99 / 100 = 999,999,999.99... → 999,999,999 (rounded down)
      // Normalized: 990,000,000
      val result = service.calculateMaxVolume(DutyRate(100_000))
      
      result.maxVolumeInMl mustBe BigDecimal("990000000")
      result.formattedForDisplay mustBe "990,000,000 ml"
    }
  }
}