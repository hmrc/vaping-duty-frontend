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

import java.time.{LocalDate, Month}
import java.time.format.TextStyle
import java.util.Locale


class BeforeYouStartViewModelSpec extends SpecBase with UnitSpec {
  
  "BeforeYouStartViewModel" - {
    val testPeriodKeyOctober = PeriodKey("27AJ")
    val testMonthOctober = Month.OCTOBER

    val testPeriodKeyDecember = PeriodKey("27AL")
    val testMonthDecember = Month.DECEMBER

    val obligationsWithFulfilled = createMockObligationsResponse().obligation

    val obligationsWithoutFulfilled = Seq(
      ObligationItem(
        identification = None,
        obligationDetails = ObligationDetails(
          openOrFulfilledStatus = ObligationStatus.O.toString,
          iCFromDate = LocalDate.of(2027, 12, 1),
          iCToDate = LocalDate.of(2027, 12, 31),
          iCDateReceived = None,
          iCDueDate = LocalDate.of(2028, 1, 25),
          periodKey = testPeriodKeyDecember.toString
        )
      )
    )

    "when user has fulfilled returns" - {
      val vm = BeforeYouStartViewModel(obligationsWithFulfilled, testPeriodKeyOctober)

      "return the correct year" in {
        val expectedResult = LocalDate.now().getYear

        vm.year mustBe expectedResult
      }

      "return the correct month due" in {
        val expectedResult = testMonthOctober.plus(1).getDisplayName(TextStyle.FULL, Locale.UK)

        vm.dueDate mustBe expectedResult
      }

      "return the correct return period month" in {
        val expectedResult = testMonthOctober.getDisplayName(TextStyle.FULL, Locale.UK)

        vm.returnPeriod mustBe expectedResult
      }

      "return Eligible for adjustmentsEligibility" in {
        vm.adjustmentsEligibility mustBe AdjustmentsEligibility.Eligible
      }
    }

    "when user has no fulfilled returns" - {
      val vm = BeforeYouStartViewModel(obligationsWithoutFulfilled, testPeriodKeyDecember)

      "return the correct year" in {
        val expectedResult = LocalDate.now().getYear

        vm.year mustBe expectedResult
      }

      "return the correct month due" in {
        val expectedResult = testMonthDecember.plus(1).getDisplayName(TextStyle.FULL, Locale.UK)

        vm.dueDate mustBe expectedResult
      }

      "return the correct return period month" in {
        val expectedResult = testMonthDecember.getDisplayName(TextStyle.FULL, Locale.UK)

        vm.returnPeriod mustBe expectedResult
      }

      "return NotEligible for adjustmentsEligibility" in {
        vm.adjustmentsEligibility mustBe AdjustmentsEligibility.NotEligible
      }
    }

    "when obligation for period key is not found" - {
      val nonExistentPeriodKey = PeriodKey("99ZZ")
      val vm = BeforeYouStartViewModel(obligationsWithFulfilled, nonExistentPeriodKey)

      "fall back to current month" in {
        val currentMonth = LocalDate.now().getMonth
        val expectedReturnPeriod = currentMonth.getDisplayName(TextStyle.FULL, Locale.UK)
        val expectedDueDate = currentMonth.plus(1).getDisplayName(TextStyle.FULL, Locale.UK)

        vm.returnPeriod mustBe expectedReturnPeriod
        vm.dueDate mustBe expectedDueDate
      }
    }
  }
}