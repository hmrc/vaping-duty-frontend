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
import models.obligations.{ObligationDetails, ObligationItem, ObligationStatus}

import java.time.LocalDate

class AdjustmentsEligibilitySpec extends SpecBase {

  private val testPeriodKey = "24AC"
  private val testFromDate = LocalDate.of(2024, 10, 1)
  private val testToDate = LocalDate.of(2024, 10, 31)
  private val testDueDate = LocalDate.of(2024, 11, 30)

  private def createObligation(status: ObligationStatus): ObligationItem = ObligationItem(
    identification = None,
    obligationDetails = ObligationDetails(
      openOrFulfilledStatus = status.toString,
      iCFromDate = testFromDate,
      iCToDate = testToDate,
      iCDateReceived = None,
      iCDueDate = testDueDate,
      periodKey = testPeriodKey
    )
  )

  "AdjustmentsEligibility.fromObligations" - {

    "must return Eligible when obligations contain fulfilled status" in {
      val obligations = Seq(createObligation(ObligationStatus.F))

      AdjustmentsEligibility.fromObligations(obligations) mustBe AdjustmentsEligibility.Eligible
    }

    "must return NotEligible when obligations only contain open status" in {
      val obligations = Seq(createObligation(ObligationStatus.O))

      AdjustmentsEligibility.fromObligations(obligations) mustBe AdjustmentsEligibility.NotEligible
    }

    "must return NotEligible when obligations list is empty" in {
      AdjustmentsEligibility.fromObligations(Seq.empty) mustBe AdjustmentsEligibility.NotEligible
    }

    "must return Eligible when at least one obligation is fulfilled among many" in {
      val obligations = Seq(
        createObligation(ObligationStatus.O),
        createObligation(ObligationStatus.F),
        createObligation(ObligationStatus.O)
      )

      AdjustmentsEligibility.fromObligations(obligations) mustBe AdjustmentsEligibility.Eligible
    }

    "must return NotEligible when multiple obligations are all open" in {
      val obligations = Seq(
        createObligation(ObligationStatus.O),
        createObligation(ObligationStatus.O),
        createObligation(ObligationStatus.O)
      )

      AdjustmentsEligibility.fromObligations(obligations) mustBe AdjustmentsEligibility.NotEligible
    }
  }

  "AdjustmentsEligibility.isEligible" - {

    "must return true for Eligible" in {
      AdjustmentsEligibility.Eligible.isEligible mustBe true
    }

    "must return false for NotEligible" in {
      AdjustmentsEligibility.NotEligible.isEligible mustBe false
    }
  }
}
