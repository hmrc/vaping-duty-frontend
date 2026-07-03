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

class SelectAdjustmentPeriodViewModelSpec extends SpecBase {

  "SelectAdjustmentPeriodViewModel" - {

    "must create view model with available periods" in {
      val obligationsResponse = ObligationsResponse(obligation = obligations(Seq(
        fulfilledObligation(october2027),
        fulfilledObligation(december2027)
      )))

      val vm = SelectAdjustmentPeriodViewModel(
        obligationsResponse,
        None,
        periodKey,
        None
      )

      vm.periods.size mustBe 2
      vm.currentYear mustBe 2027
    }

    "must filter out current return period" in {
      val obligationsResponse = ObligationsResponse(obligation = obligations(Seq(
        fulfilledObligation(periodKey),
        fulfilledObligation(october2027)
      )))

      val vm = SelectAdjustmentPeriodViewModel(
        obligationsResponse,
        None,
        periodKey,
        None
      )

      vm.periods.size mustBe 1
      vm.periods.head.href.value must include(october2027.value)
    }

    "must filter out periods already in adjustment list" in {
      val adjustment = AdjustmentEntry(
        period = october2027,
        adjustmentType = AdjustmentType.UnderDeclared,
        volumeInMl = BigDecimal("100.0")
      )
      val adjustmentList = AdjustmentList(Seq(adjustment))
      
      val obligationsResponse = ObligationsResponse(obligation = obligations(Seq(
        fulfilledObligation(october2027),
        fulfilledObligation(december2027)
      )))

      val vm = SelectAdjustmentPeriodViewModel(
        obligationsResponse,
        None,
        periodKey,
        Some(adjustmentList)
      )

      vm.periods.size mustBe 1
      vm.periods.head.href.value must include(december2027.value)
    }

    "must handle empty obligations" in {
      val obligationsResponse = ObligationsResponse(obligation = Seq.empty)

      val vm = SelectAdjustmentPeriodViewModel(
        obligationsResponse,
        None,
        periodKey,
        None
      )

      vm.periods mustBe empty
      vm.paginationItems mustBe empty
    }
  }
}
