/*
 * Copyright 2025 HM Revenue & Customs
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

package viewmodels.returns

import base.{SpecBase, UnitSpec}
import models.identifiers.PeriodKey
import models.obligations.ObligationDetails
import models.returns.AdjustmentsEligibility
import viewmodels.returns.submit.BeforeYouStartViewModel


class BeforeYouStartViewModelSpec extends SpecBase with UnitSpec {
  
  "BeforeYouStartViewModel" - {

    "when user has fulfilled returns" - {
      val obligationsWithFulfilled = obligations(
        Seq(
          openObligation(december2027),
          openObligation(november2027),
          fulfilledObligation(october2027)
        )
      )
      val vm = BeforeYouStartViewModel(obligationsWithFulfilled, november2027).get

      "return the correct year of the return" in {
        vm.yearOfReturn mustBe 2027
      }

      "return the correct year the return is due" in {
        vm.dueYear mustBe 2027
      }

      "return the correct month due" in {
        vm.dueDate mustBe "7 December"
      }

      "return the correct return period month" in {
        vm.returnPeriod mustBe "November"
      }

      "return Eligible for adjustmentsEligibility" in {
        vm.adjustmentsEligibility mustBe AdjustmentsEligibility.Eligible
      }
    }

    "when user has no fulfilled returns" - {
      val obligationsWithoutFulfilled = obligations(
        Seq(openObligation(december2027))
      )
      val vm = BeforeYouStartViewModel(obligationsWithoutFulfilled, december2027).get

      "return the correct month due" in {
        vm.dueDate mustBe "7 January"
      }

      "return the correct return period month" in {
        vm.returnPeriod mustBe "December"
      }

      "return NotEligible for adjustmentsEligibility" in {
        vm.adjustmentsEligibility mustBe AdjustmentsEligibility.NotEligible
      }
    }

    "when obligation for period key is not found" - {
      val nonExistentPeriodKey = PeriodKey("99ZZ")

      "return None" in {
        val anyOldObligations = obligations(
          Seq(openObligation(december2027))
        )
        val result = BeforeYouStartViewModel(anyOldObligations, nonExistentPeriodKey)
        
        result mustBe None
      }
    }
  }
}