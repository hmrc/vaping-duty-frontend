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
import models.obligations.{ObligationDetails, ObligationItem, ObligationStatus}
import viewmodels.returns.submit.ConfirmationViewModel

import java.time.LocalDate
import java.time.format.DateTimeFormatter

class ConfirmationViewModelSpec extends SpecBase with UnitSpec {
  
  private def createObligation(): ObligationItem = {
    ObligationItem(
      identification = None,
      obligationDetails = ObligationDetails(
        openOrFulfilledStatus = ObligationStatus.F.toString,
        iCFromDate = LocalDate.of(2026, 1, 1),
        iCToDate = LocalDate.of(2026, 1, 31),
        iCDateReceived = Some(LocalDate.of(2026, 2, 1)),
        iCDueDate = LocalDate.of(2026, 2, 7),
        periodKey = periodKey.value
      )
    )
  }

  "ConfirmationViewModel" - {

    val returnsResponse = createReturnDisplayResponse()
    val obligation = createObligation().obligationDetails
    
    "must extract and format submission date correctly" in {
      val vm = ConfirmationViewModel(returnsResponse, obligation, btaLink)

      val expectedFormat = DateTimeFormatter.ofPattern("d MMMM yyyy")
      val expectedDate = java.time.LocalDate.ofInstant(
        returnsResponse.success.processingDate,
        java.time.ZoneId.of("Europe/London")
      ).format(expectedFormat)

      vm.submissionDate mustBe expectedDate
    }

    "must extract and format period month/year correctly" in {
      val vm = ConfirmationViewModel(returnsResponse, obligation, btaLink)

      val expectedFormat = DateTimeFormatter.ofPattern("MMMM yyyy")
      val expectedPeriod = obligation.iCFromDate.format(expectedFormat)

      vm.periodMonthYear mustBe expectedPeriod
    }
    
    "must extract total duty amount correctly" in {
      val vm = ConfirmationViewModel(returnsResponse, obligation, btaLink)

      vm.totalDutyAmount mustBe returnsResponse.success.totalDutyDue.get.totalDutyDue
    }

    "must extract and uppercase charge reference when present" in {
      val vm = ConfirmationViewModel(returnsResponse, obligation, btaLink)

      vm.chargeReference mustBe defined
      vm.chargeReference.get mustBe returnsResponse.success.chargeDetails.get.chargeReference.get.toUpperCase
    }

    "must handle missing charge reference" in {
      val responseWithoutChargeRef = returnsResponse.copy(
        success = returnsResponse.success.copy(
          chargeDetails = Some(returnsResponse.success.chargeDetails.get.copy(chargeReference = None))
        )
      )

      val vm = ConfirmationViewModel(responseWithoutChargeRef, obligation, btaLink)

      vm.chargeReference mustBe None
    }

    "must generate correct content for negative duty amount (user is owed money)" in {
      val negativeAmount = BigDecimal(-150.50)
      val responseWithNegativeAmount = returnsResponse.copy(
        success = returnsResponse.success.copy(
          totalDutyDue = Some(returnsResponse.success.totalDutyDue.get.copy(totalDutyDue = negativeAmount))
        )
      )

      val vm = ConfirmationViewModel(responseWithNegativeAmount, obligation, btaLink)

      vm.totalDutyAmount mustBe negativeAmount
      vm.content.toString must include("You are owed")
      vm.content.toString must include("£150.50")
      vm.content.toString must include("repayment")
      vm.content.toString must include("This amount will be credited to your next Vaping Duty return")
    }

    "must generate correct content for positive duty amount (user owes money)" in {
      val vm = ConfirmationViewModel(returnsResponse, obligation, btaLink)

      vm.totalDutyAmount must be > BigDecimal(0)
      vm.content.toString must include("You must pay")
      vm.content.toString must include("business tax account")
    }

    "must generate correct content for zero duty amount" in {
      val zeroAmount = BigDecimal(0)
      val responseWithZeroAmount = returnsResponse.copy(
        success = returnsResponse.success.copy(
          totalDutyDue = Some(returnsResponse.success.totalDutyDue.get.copy(totalDutyDue = zeroAmount))
        )
      )

      val vm = ConfirmationViewModel(responseWithZeroAmount, obligation, btaLink)

      vm.totalDutyAmount mustBe zeroAmount
      vm.content.toString must include("You have nothing to pay for this return period")
    }
  }
}
