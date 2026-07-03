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

package viewmodels.returns.submit.adjustments

import base.SpecBase
import models.obligations.ObligationsResponse
import models.returns.adjustments.{AdjustmentEntry, AdjustmentList, AdjustmentType}

class AdjustmentCheckYourAnswersViewModelSpec extends SpecBase {

  val dutyRate = BigDecimal("3.00")
  val dutyRatesMap = Map(october2027.toString -> dutyRate)

  "AdjustmentCheckYourAnswersViewModel" - {

    "must create view model with correct summary cards for under declared adjustment" in {
      val adjustment = AdjustmentEntry(
        period = october2027,
        adjustmentType = AdjustmentType.UnderDeclared,
        volumeInMl = BigDecimal("100.0")
      )
      val adjustmentList = AdjustmentList(Seq(adjustment))
      val obligationsResponse = ObligationsResponse(obligation = obligations(Seq(fulfilledObligation(october2027))))

      val vm = AdjustmentCheckYourAnswersViewModel(
        Some(adjustmentList),
        obligationsResponse,
        periodKey,
        dutyRatesMap
      )

      vm.hasAdjustments mustBe true
      vm.summaryCards.size mustBe 1
      vm.summaryCards.head.rows.size mustBe 3
    }

    "must calculate correct total adjustment for under declared" in {
      val adjustment = AdjustmentEntry(
        period = october2027,
        adjustmentType = AdjustmentType.UnderDeclared,
        volumeInMl = BigDecimal("1000.0")
      )
      val adjustmentList = AdjustmentList(Seq(adjustment))
      val obligationsResponse = ObligationsResponse(obligation = obligations(Seq(fulfilledObligation(october2027))))

      val vm = AdjustmentCheckYourAnswersViewModel(
        Some(adjustmentList),
        obligationsResponse,
        periodKey,
        dutyRatesMap
      )

      vm.totalAdjustment mustBe BigDecimal("300.00")
    }

    "must calculate correct total adjustment for over declared (negative)" in {
      val adjustment = AdjustmentEntry(
        period = october2027,
        adjustmentType = AdjustmentType.OverDeclared,
        volumeInMl = BigDecimal("1000.0")
      )
      val adjustmentList = AdjustmentList(Seq(adjustment))
      val obligationsResponse = ObligationsResponse(obligation = obligations(Seq(fulfilledObligation(october2027))))

      val vm = AdjustmentCheckYourAnswersViewModel(
        Some(adjustmentList),
        obligationsResponse,
        periodKey,
        dutyRatesMap
      )

      vm.totalAdjustment mustBe BigDecimal("-300.00")
    }

    "must handle empty adjustment list" in {
      val obligationsResponse = ObligationsResponse(obligation = obligations(Seq(fulfilledObligation(october2027))))

      val vm = AdjustmentCheckYourAnswersViewModel(
        None,
        obligationsResponse,
        periodKey,
        dutyRatesMap
      )

      vm.hasAdjustments mustBe false
      vm.summaryCards mustBe empty
      vm.totalAdjustment mustBe BigDecimal("0")
    }
  }
}
