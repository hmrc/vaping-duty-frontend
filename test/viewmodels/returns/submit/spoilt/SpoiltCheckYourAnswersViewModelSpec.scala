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

package viewmodels.returns.submit.spoilt

import base.SpecBase
import models.returns.SpoiltVolumeByPeriod

class SpoiltCheckYourAnswersViewModelSpec extends SpecBase {

  val dutyRate: BigDecimal = BigDecimal("3.00")
  val dutyRatesMap: Map[String, BigDecimal] = Map(october2027.toString -> dutyRate)

  "SpoiltCheckYourAnswersViewModel" - {

    "must create view model with correct summary cards for a declared spoilt entry" in {
      val entry = SpoiltVolumeByPeriod(volume = BigDecimal("100.0"), periodKey = october2027)
      val obligationDetails = obligations(Seq(fulfilledObligation(october2027))).map(_.obligationDetails)

      val vm = SpoiltCheckYourAnswersViewModel(
        Some(true),
        Some(List(entry)),
        obligationDetails,
        periodKey,
        dutyRatesMap,
        returnsDateUtils
      )

      vm.hasSpoiltProducts mustBe true
      vm.summaryCards.size mustBe 1
      vm.summaryCards.head.rows.size mustBe 3
    }

    "must calculate correct total spoilt duty" in {
      val entry = SpoiltVolumeByPeriod(volume = BigDecimal("1000.0"), periodKey = october2027)
      val obligationDetails = obligations(Seq(fulfilledObligation(october2027))).map(_.obligationDetails)

      val vm = SpoiltCheckYourAnswersViewModel(
        Some(true),
        Some(List(entry)),
        obligationDetails,
        periodKey,
        dutyRatesMap,
        returnsDateUtils
      )

      vm.totalSpoiltDuty mustBe BigDecimal("3000.00")
      vm.formattedTotalSpoiltDuty mustBe "£3,000"
    }

    "must handle no spoilt products declared" in {
      val obligationDetails = obligations(Seq(fulfilledObligation(october2027))).map(_.obligationDetails)

      val vm = SpoiltCheckYourAnswersViewModel(
        Some(false),
        None,
        obligationDetails,
        periodKey,
        dutyRatesMap,
        returnsDateUtils
      )

      vm.hasSpoiltProducts mustBe false
      vm.summaryCards.size mustBe 1
      vm.summaryCards.head.rows.size mustBe 1
      vm.totalSpoiltDuty mustBe BigDecimal("0")
    }

    "must handle an empty spoilt list" in {
      val obligationDetails = obligations(Seq(fulfilledObligation(october2027))).map(_.obligationDetails)

      val vm = SpoiltCheckYourAnswersViewModel(
        Some(true),
        Some(List.empty),
        obligationDetails,
        periodKey,
        dutyRatesMap,
        returnsDateUtils
      )

      vm.hasSpoiltProducts mustBe false
      vm.summaryCards.size mustBe 0
      vm.totalSpoiltDuty mustBe BigDecimal("0")
    }
  }

  "hasAvailablePeriodsToAdd" - {

    "must return true when a fulfilled period is not in the spoilt list" in {
      val obligationDetails = obligations(Seq(fulfilledObligation(october2027), fulfilledObligation(december2027))).map(_.obligationDetails)
      val spoiltList = Some(List(SpoiltVolumeByPeriod(volume = BigDecimal("100.0"), periodKey = october2027)))

      SpoiltCheckYourAnswersViewModel.hasAvailablePeriodsToAdd(obligationDetails, periodKey, spoiltList) mustBe true
    }

    "must return false when every fulfilled period is already in the spoilt list" in {
      val obligationDetails = obligations(Seq(fulfilledObligation(october2027))).map(_.obligationDetails)
      val spoiltList = Some(List(SpoiltVolumeByPeriod(volume = BigDecimal("100.0"), periodKey = october2027)))

      SpoiltCheckYourAnswersViewModel.hasAvailablePeriodsToAdd(obligationDetails, periodKey, spoiltList) mustBe false
    }
  }
}
