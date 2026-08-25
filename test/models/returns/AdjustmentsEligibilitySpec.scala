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
import builders.ObligationsBuilders

class AdjustmentsEligibilitySpec extends SpecBase with ObligationsBuilders {

  "AdjustmentsEligibility.fromObligations" - {

    "must return Eligible when obligations contain fulfilled status" in {
      val testObligations = Seq(fulfilledObligation(march2024))

      AdjustmentsEligibility.fromObligations(testObligations) mustBe AdjustmentsEligibility.Eligible
    }

    "must return NotEligible when obligations only contain open status" in {
      val testObligations = Seq(openObligation(march2024))

      AdjustmentsEligibility.fromObligations(testObligations) mustBe AdjustmentsEligibility.NotEligible
    }

    "must return NotEligible when obligations list is empty" in {
      AdjustmentsEligibility.fromObligations(Seq.empty) mustBe AdjustmentsEligibility.NotEligible
    }

    "must return Eligible when at least one obligation is fulfilled among many" in {
      val testObligations = Seq(
        openObligation(january2024),
        fulfilledObligation(february2024),
        openObligation(march2024)
      )

      AdjustmentsEligibility.fromObligations(testObligations) mustBe AdjustmentsEligibility.Eligible
    }

    "must return NotEligible when multiple obligations are all open" in {
      val testObligations = Seq(
        openObligation(january2024),
        openObligation(february2024),
        openObligation(march2024)
      )

      AdjustmentsEligibility.fromObligations(testObligations) mustBe AdjustmentsEligibility.NotEligible
    }
  }

  "AdjustmentsEligibility.fromObligationDetails" - {

    "must return Eligible when obligation details contain fulfilled status" in {
      val details = Seq(fulfilledObligation(march2024))

      AdjustmentsEligibility.fromObligationDetails(details) mustBe AdjustmentsEligibility.Eligible
    }

    "must return NotEligible when obligation details only contain open status" in {
      val details = Seq(openObligation(march2024))

      AdjustmentsEligibility.fromObligationDetails(details) mustBe AdjustmentsEligibility.NotEligible
    }

    "must return NotEligible when obligation details list is empty" in {
      AdjustmentsEligibility.fromObligationDetails(Seq.empty) mustBe AdjustmentsEligibility.NotEligible
    }

    "must return Eligible when at least one obligation detail is fulfilled among many" in {
      val details = Seq(
        openObligation(january2024),
        fulfilledObligation(february2024),
        openObligation(march2024)
      )

      AdjustmentsEligibility.fromObligationDetails(details) mustBe AdjustmentsEligibility.Eligible
    }

    "must return NotEligible when multiple obligation details are all open" in {
      val details = Seq(
        openObligation(january2024),
        openObligation(february2024),
        openObligation(march2024)
      )

      AdjustmentsEligibility.fromObligationDetails(details) mustBe AdjustmentsEligibility.NotEligible
    }
  }
}
