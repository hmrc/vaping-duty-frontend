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

package viewmodels.returns.submit

import base.SpecBase
import models.identifiers.PeriodKey
import models.obligations.ObligationsResponse

class AdjustmentVolumeWithTypeViewModelSpec extends SpecBase {

  "AdjustmentVolumeWithTypeViewModel" - {

    "must return the formatted month and year when a matching obligation is found" in {
      val obligationsResponse = ObligationsResponse(obligation = obligations(Seq(fulfilledObligation(october2027))))
      val vm = AdjustmentVolumeWithTypeViewModel(obligationsResponse, october2027)

      vm.periodDisplay mustBe "October 2027"
    }

    "must throw IllegalStateException when no matching obligation is found" in {
      val unknownPeriodKey = PeriodKey("99ZZ")
      val obligationsResponse = ObligationsResponse(obligation = obligations(Seq(fulfilledObligation(october2027))))

      val exception = intercept[IllegalStateException] {
        AdjustmentVolumeWithTypeViewModel(obligationsResponse, unknownPeriodKey)
      }

      exception.getMessage must include("Period key '99ZZ' not found in obligations")
      exception.getMessage must include("Available period keys:")
    }

    "must throw IllegalStateException when obligations list is empty" in {
      val obligationsResponse = ObligationsResponse(obligation = Seq.empty)

      val exception = intercept[IllegalStateException] {
        AdjustmentVolumeWithTypeViewModel(obligationsResponse, october2027)
      }

      exception.getMessage must include(s"Period key '${october2027.value}' not found in obligations")
      exception.getMessage must include("Available period keys: none")
    }

    "must return the correct display for all twelve months" in {
      val allMonthPeriodKeys = Seq(
        PeriodKey("26AA") -> "January 2026",
        PeriodKey("26AB") -> "February 2026",
        PeriodKey("26AC") -> "March 2026",
        PeriodKey("26AD") -> "April 2026",
        PeriodKey("26AE") -> "May 2026",
        PeriodKey("26AF") -> "June 2026",
        PeriodKey("26AG") -> "July 2026",
        PeriodKey("26AH") -> "August 2026",
        PeriodKey("26AI") -> "September 2026",
        PeriodKey("26AJ") -> "October 2026",
        PeriodKey("26AK") -> "November 2026",
        PeriodKey("26AL") -> "December 2026"
      )

      allMonthPeriodKeys.foreach { case (key, expectedDisplay) =>
        val obligationsResponse = ObligationsResponse(obligation = obligations(Seq(fulfilledObligation(key))))
        val vm = AdjustmentVolumeWithTypeViewModel(obligationsResponse, key)
        vm.periodDisplay mustBe expectedDisplay
      }
    }
  }
}
