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
import models.obligations.{ObligationDetails, ObligationItem, ObligationStatus}
import models.returns.AdjustmentsEligibility
import viewmodels.returns.submit.BeforeYouStartViewModel

import java.time.LocalDate


class BeforeYouStartViewModelSpec extends SpecBase with UnitSpec {
  
  "BeforeYouStartViewModel" - {
    val october2027 = PeriodKey("27AJ")
    val december2027 = PeriodKey("27AL")

    val obligationsWithFulfilled = createMockObligations()

    val obligationsWithoutFulfilled = Seq(
      ObligationItem(
        identification = None,
        obligationDetails = ObligationDetails(
          openOrFulfilledStatus = ObligationStatus.O.toString,
          iCFromDate = LocalDate.of(2027, 12, 1),
          iCToDate = LocalDate.of(2027, 12, 31),
          iCDateReceived = None,
          iCDueDate = LocalDate.of(2028, 1, 7),
          periodKey = december2027.toString
        )
      )
    )

    "when user has fulfilled returns" - {
      val vm = BeforeYouStartViewModel(obligationsWithFulfilled, october2027).get

      "return the correct year of the return" in {
        vm.yearOfReturn mustBe 2027
      }

      "return the correct year the return is due" in {
        vm.dueYear mustBe 2027
      }

      "return the correct month due" in {
        vm.dueDate mustBe "7 November"
      }

      "return the correct return period month" in {
        vm.returnPeriod mustBe "October"
      }

      "return Eligible for adjustmentsEligibility" in {
        vm.adjustmentsEligibility mustBe AdjustmentsEligibility.Eligible
      }
    }

    "when user has no fulfilled returns" - {
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
        val result = BeforeYouStartViewModel(obligationsWithFulfilled, nonExistentPeriodKey)
        
        result mustBe None
      }
    }
  }
}